package com.virjar.dungproxy.server.proxyservice.client.listener;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.virjar.dungproxy.server.model.ProxyModel;
import com.virjar.dungproxy.server.proxyservice.client.decoder.HttpHeaderDecoder;
import com.virjar.dungproxy.server.proxyservice.client.exception.ServerChannelInactiveException;
import com.virjar.dungproxy.server.proxyservice.client.exception.ServerChannelNotWritableException;
import com.virjar.dungproxy.server.proxyservice.common.util.NetworkUtil;
import com.virjar.dungproxy.server.proxyservice.handler.ExceptionCaughtHandler;
import com.virjar.dungproxy.server.proxyservice.handler.SocksInitResponseHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.socks.SocksAuthScheme;
import io.netty.handler.codec.socks.SocksInitRequest;
import io.netty.handler.codec.socks.SocksInitResponseDecoder;
import io.netty.handler.codec.socks.SocksMessageEncoder;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.AttributeKey;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.TimerTask;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;



public class DefaultRequestExecutor implements RequestExecutor {

    private static final Logger log = LoggerFactory.getLogger(DefaultRequestExecutor.class);

    public final static AttributeKey<Boolean> KEEP_ALIVE = AttributeKey.valueOf("keep-alive");

    private static final HashedWheelTimer timeoutManager = new HashedWheelTimer(new ThreadFactoryBuilder().setNameFormat("RE-timeout-%s").build());

    private static final long HAND_SHAKE_TIMEOUT = 20000;

    private static final long SSL_CLOSE_NOTIFY_TIMEOUT = 6000;

    private static final AttributeKey<String> CONNECTION_POOL_KEY = AttributeKey.valueOf("connectionPoolKey");

    private ResponseListener listener;

    private NioSocketChannel serverChannel;

    private SimpleConnectionsPool connectionsPool;

    private int readTimeoutMs;
    private int requestTimeoutMs;
    private int connectionTimeoutMs;
    private int writeBufferLowWaterMark;
    private int writeBufferHighWaterMark;
    private final boolean isHttps;
    private EventLoopGroup workerGroup;
    private FullHttpRequest request;
    private SslContext sslContext;
    private ProxyModel proxyServer;
    private String relativePathForHttps = null;
    private TimeoutsHolder timeoutsHolder;
    private volatile AtomicBoolean requesting = new AtomicBoolean(false);
    private final boolean customAuth;
    private final boolean retry;

    public DefaultRequestExecutor(
            int readTimeoutMs,
            int requestTimeoutMs,
            int connectionTimeoutMs,
            int writeBufferLowWaterMark,
            int writeBufferHighWaterMark,
            EventLoopGroup workerGroup,
            FullHttpRequest request,
            ResponseListener listener,
            SslContext sslContext,
            ProxyModel proxyServer,
            SimpleConnectionsPool connectionsPool,
            boolean customAuth,
            boolean retry) {
        this.listener = listener;
        this.readTimeoutMs = readTimeoutMs;
        this.requestTimeoutMs = requestTimeoutMs;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.workerGroup = workerGroup;
        this.request = (FullHttpRequest) request.duplicate();
        this.writeBufferLowWaterMark = writeBufferLowWaterMark;
        this.writeBufferHighWaterMark = writeBufferHighWaterMark;
        this.sslContext = sslContext;
        this.proxyServer = proxyServer;
        this.isHttps = NetworkUtil.isSchemaHttps(request.getUri());
        this.connectionsPool = connectionsPool;
        this.listener.setRequestExecutor(this);
        this.timeoutsHolder = new TimeoutsHolder();
        this.customAuth = customAuth;
        this.retry = retry;
    }


    @Override
    public RequestExecutorProxy execute() {
        if (requesting.compareAndSet(false, true)) {
            connect(proxyServer.getProxyIp(), proxyServer.getPort());
        } else {
            throw new IllegalStateException("RequestExecutor could not execute requests concurrently");
        }
        return new RequestExecutorProxy(this);
    }

    @Override
    public void cancel(boolean keepServerChannel) {
        log.debug("Cancel request [{}]", request.getUri());
        if (timeoutsHolder != null) {
            timeoutsHolder.cancel();
        }
        if (!keepServerChannel && serverChannel != null) {
            serverChannel.close();
        }
    }

    private class TimeoutsHolder {

        private final AtomicBoolean cancelled = new AtomicBoolean();
        public Timeout idleTimeout;
        public Timeout requestTimeout;

        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                if (requestTimeout != null) {
                    log.debug("cancel task " + requestTimeout);
                    requestTimeout.cancel();
                    requestTimeout = null;
                }
                if (idleTimeout != null) {
                    log.debug("cancel task " + idleTimeout);
                    idleTimeout.cancel();
                    idleTimeout = null;
                }
            }
        }
    }

    private class TimeoutTask implements TimerTask {

        private String name = "";

        public TimeoutTask(String name) {
            this.name = name;
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            log.debug("Task [{}] timeout", name);
            if (serverChannel != null) {
                serverChannel.close();
            }
            listener.onRequestTimeout();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Override
    public ProxyModel getProxyServer() {
        return proxyServer;
    }

    private void connect(final String proxyHost, final int proxyPort) {
        if (tryToUseCachedConnection(proxyHost, proxyPort)) {
            log.debug("use cached connection for uri [{}] through proxy [{}:{}]", request.uri(), proxyHost, proxyPort);
            return;
        } else {
            if (connectionsPool != null && !connectionsPool.canCacheConnection()) {
                log.error("Too many connections. proxyHost:{}, proxyPort:{}", proxyHost, proxyPort);
                listener.onConnectionPoolIsFull();
            }
            log.debug("use new connection for uri [{}] through proxy [{}:{}]", request.uri(), proxyHost, proxyPort);
        }

        log.debug("Starting to execute request [{}] through proxy [{}:{}]", request, proxyHost, proxyPort);

        // Use no cache
        Bootstrap bootstrap = new Bootstrap();
        final boolean isSocks = proxyServer.getType() == 4;
        bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                //禁用nagle算法
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_REUSEADDR, true)
                // false 代表通道建立起来之后并不想读取消息，也许只是发消息，或者手工，或者特定条件下读取消息
                .option(ChannelOption.AUTO_READ, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMs)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT)
                .option(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT)
                .handler(new ChannelInitializer<NioSocketChannel>() {

                    @Override
                    protected void initChannel(NioSocketChannel ch) throws Exception {
                        if (isSocks) {
                            addHandlersForSocksProxy(ch.pipeline());
                        } else {
                            addHandlersForHttpProxy(ch.pipeline());
                        }
                    }
                });
        if (isSocks) {
            bootstrap.connect(proxyHost, proxyPort).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    // 暂时不使用连接池
                    ResponseListener.Result result = new ResponseListener.Result(future.isSuccess(), future.cause());
                    result.setAttr(Boolean.FALSE);
                    if (listener.onConnectCompleted(result).equals(ResponseListener.State.CONTINUE)) {
                        future.channel().config().setAutoRead(true);
                        future.channel().writeAndFlush(new SocksInitRequest(Lists.newArrayList(SocksAuthScheme.NO_AUTH)));
                        createTimeoutTask();
                        setServerChannel((NioSocketChannel) future.channel());
                    } else {
                        Channel channel = future.channel();
                        if (channel != null && channel.isActive()) { //这里不关疑似会泄漏连接。
                            channel.close();
                        }
                    }
                }
            });
        } else {
            bootstrap.connect(proxyHost, proxyPort).addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    try {
                        ResponseListener.Result result = new ResponseListener.Result(future.isSuccess(), future.cause());
                        result.setAttr(Boolean.FALSE);
                        if (listener.onConnectCompleted(result).equals(ResponseListener.State.CONTINUE)) {
                            createTimeoutTask();
                            log.debug("Connection to proxy [{}:{}] established", proxyHost, proxyPort);
                            NioSocketChannel serverChannelToSet = (NioSocketChannel) future.channel();
                            serverChannelToSet.attr(CONNECTION_POOL_KEY).set(buildPoolKey(proxyHost, proxyPort).get());
                            setServerChannel(serverChannelToSet);
                            sendRequest(request, isHttps);
                        } else {
                            Channel channel = future.channel();
                            if (channel != null && channel.isActive()) { //这里不关疑似会泄漏连接。
                                channel.close();
                            }
                        }
                    } catch (Exception e) {
                        // FIXME 这里有 NullPointer, 疑似是并发问题, 查看 catalina.out 有记录
                        log.error("strange exception", e);
                        throw e;
                    }
                }
            });
        }
    }

    private void addHandlersForHttpProxy(ChannelPipeline pipeline) {
        if (isHttps) {
            pipeline.addLast(
                    // new LoggingHandler(),
                    new HttpClientCodec(),
                    new HttpObjectAggregator(1024 * 1024),
                    new ConnectFinishHandler()
            );
        } else {
            addHandlersForHttpRequests(pipeline);
        }
    }

    public void continueRequestAfterSocksInit(Channel channel) {
        if (isHttps()) {
            startHttpsRequest(channel);
        } else {
            channel.pipeline().addLast(
                    new HttpRequestEncoder(),
                    new HttpHeaderDecoder(4096, 4096 * 4, listener, retry, proxyServer)
            );
            String path = URI.create(request.uri()).getRawPath();
            request.setUri(path);
            request.retain();
            channel.writeAndFlush(request);
        }
    }

    private void addHandlersForSocksProxy(ChannelPipeline pipeline) {
        pipeline.addLast(new LoggingHandler())
                .addLast(new SocksMessageEncoder())
                .addLast(new SocksInitResponseDecoder())
                .addLast(new SocksInitResponseHandler(listener, request, this));
    }

    private void createTimeoutTask() {
        String host = request.headers().get("Host");
        String timeoutName = "";
        if (log.isDebugEnabled()) {
            timeoutName = host == null ? "" : host + "|" + proxyServer.getIp() + ":" + proxyServer.getPort();
        }
        timeoutsHolder.requestTimeout = timeoutManager.newTimeout(new TimeoutTask(timeoutName), requestTimeoutMs, TimeUnit.MILLISECONDS);

    }

    private void addHandlersForHttpRequests(ChannelPipeline pipeline) {
        pipeline.addLast(
                new HttpRequestEncoder(),
                new HttpHeaderDecoder(4096, 4096 * 4, listener, retry, proxyServer)
        );
    }

    private boolean tryToUseCachedConnection(String proxyHost, int proxyPort) {
        if (connectionsPool == null)  {
            return false;
        }
        Optional<String> poolKey = buildPoolKey(proxyHost, proxyPort);
        if (!poolKey.isPresent()) {
            return false;
        }
        Channel cachedChannel = connectionsPool.poll(poolKey.get());
        if (cachedChannel == null) {
            return false;
        }
        cachedChannel.config().setAutoRead(true);
        if (isHttps) {
            URI uri;
            try {
                uri = new URI(request.uri());
            } catch (URISyntaxException e) {
                log.error("uri format error", e);
                listener.onThrowable("Send https request", new IllegalStateException("Uri format error"));
                if (!connectionsPool.offer(poolKey.get(), cachedChannel)) {
                    cachedChannel.close();
                }
                return false;
            }
            String relativePath = uri.getRawPath();
            if (uri.getRawQuery() != null) {
                relativePath = relativePath + "?" + uri.getRawQuery();
            }
            this.relativePathForHttps = relativePath;
        }

        resetHandlers(cachedChannel, isHttps);
        setServerChannel((NioSocketChannel) cachedChannel);
        createTimeoutTask();
        ResponseListener.Result result = new ResponseListener.Result(true, null);
        result.setAttr(Boolean.TRUE);
        listener.onConnectCompleted(result);
        sendRawRequest(request);//不需要再connect了
        return true;
    }

    private void resetHandlers(Channel channel, boolean useSsl) {
        ChannelPipeline pipeline = channel.pipeline();
        addHandlersForHttpRequests(pipeline);
        ExceptionCaughtHandler exceptionCaughtHandler = pipeline.get(ExceptionCaughtHandler.class);
        if (exceptionCaughtHandler != null) {
            exceptionCaughtHandler.setIgnoreException(false);
        }
    }
    private Optional<String> buildPoolKey(String proxyHost, int proxyPort) {
        if (!isHttps) {
            return Optional.of(proxyHost + ":" + proxyPort);
        }
        String requestHost;
        int requestPort;
        try {
            URI uri = new URI(request.uri());
            requestHost = uri.getHost();
            requestPort = uri.getPort();
            if (requestPort <= 0) {
                requestPort = 443;
            }
        } catch (URISyntaxException e) {
            log.error("invalid uri:{}", request.uri(), e);
            return Optional.absent();
        }
        return Optional.of(proxyHost + ":" + proxyPort + " " + requestHost + ":" + requestPort);
    }

    protected void sendRawRequest(final FullHttpRequest request) {
        if (isHttps && relativePathForHttps != null) {
            request.setUri(relativePathForHttps);
        }
        boolean ra = !serverChannel.isActive();
        boolean rw = !serverChannel.isWritable();

        if ((ra || rw) && listener.isCompleted()) {
            cancel(false);
            if (ra) {
                listener.onThrowable("Server write channel inactive", ServerChannelInactiveException.INSTANCE);
            } else {
                listener.onThrowable("Server write channel not writable", ServerChannelNotWritableException.INSTANCE);
            }
        } else {
            request.retain();
            serverChannel.writeAndFlush(request).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        log.debug("Request sent to proxy server successfully");
                        listener.onRequestSent();
                        future.channel().pipeline().remove(HttpRequestEncoder.class);
                        if (!serverChannel.config().isAutoRead()) {
                            serverChannel.read();
                        }
                    } else {
                        String msg = "Http request sent to proxy server fail";
                        log.debug(msg);
                        listener.onThrowable(msg, future.cause());
                    }
                }
            });
        }
    }

    protected void sendHttpsCONNECT(FullHttpRequest request) {
        String host;
        int port;
        URI uri;
        try {
            uri = new URI(request.getUri());
        } catch (URISyntaxException e) {
            log.error("uri format error", e);
            listener.onThrowable("Send https request", new IllegalStateException("Uri format error"));
            return;
        }
        String relativePath = uri.getRawPath();
        if (uri.getRawQuery() != null) {
            relativePath = relativePath + "?" + uri.getRawQuery();
        }
        this.relativePathForHttps = relativePath;
        host = uri.getHost();
        port = uri.getPort();
        String hostInHeader = request.headers().get(HttpHeaderNames.HOST);
        if (port < 0) port = NetworkUtil.getPortFromHostHeader(hostInHeader);
        if (port < 0) port = 443;
        FullHttpRequest connectRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.CONNECT, host + ":" + port);
        if (hostInHeader != null) {
            connectRequest.headers().add(HttpHeaderNames.HOST, hostInHeader);
        }
        serverChannel.writeAndFlush(connectRequest).addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.debug("Request CONNECT sent to proxy server successfully");
                    if (!serverChannel.config().isAutoRead()) {
                        serverChannel.read();
                    }
                } else {
                    log.warn("Request CONNECT sent to proxy server fail");
                    listener.onThrowable(getClass().getName() + " sendHttpsRequest -> serverWrite", future.cause());
                }
            }
        });
    }

    private class ConnectFinishHandler extends SimpleChannelInboundHandler<FullHttpResponse> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpResponse msg) throws Exception {
            int statusCode = msg.status().code();
            if (statusCode != 200) {
                listener.onThrowable("Receive illegal response while executing CONNECT request, expect: 200 actual: " + statusCode, null);
                return;
            }
            startHttpsRequest(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            listener.onThrowable(getClass().getName() + " channelInactive()", null);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            listener.onThrowable(getClass().getName() + " exceptionCaught()", cause);
        }
    }

    private void startHttpsRequest(Channel channel) {
        if (!channel.config().isAutoRead()) {
            channel.config().setAutoRead(true);
        }
        final ChannelPipeline pipeline = channel.pipeline();
//            SSLEngine sslEngine = sslContext.newEngine(ctx.alloc(), proxyServer.getHost(), proxyServer.getPort());
        SSLEngine engine = sslContext.newEngine(channel.alloc());
        engine.setUseClientMode(true);
        SslHandler sslHandler = new SslHandler(engine);
        sslHandler.setHandshakeTimeoutMillis(HAND_SHAKE_TIMEOUT);
        sslHandler.setCloseNotifyTimeoutMillis(SSL_CLOSE_NOTIFY_TIMEOUT);
        pipeline.addFirst(sslHandler);
        sslHandler.handshakeFuture().addListener(new GenericFutureListener<Future<? super Channel>>() {
            @Override
            public void operationComplete(Future<? super Channel> future) throws Exception {
                if (future.isSuccess()) {
                    listener.onHandshakeSuccess();
                    NetworkUtil.removeHandler(pipeline, HttpClientCodec.class);
                    NetworkUtil.removeHandler(pipeline, HttpObjectAggregator.class);
                    NetworkUtil.removeHandler(pipeline, ConnectFinishHandler.class);
                    pipeline.addLast(
                            new HttpRequestEncoder(),
                            new HttpHeaderDecoder(4096, 4096 * 4, listener, retry, proxyServer)
                    );
                    serverChannel.config().setAutoRead(true);
                    sendRawRequest(request);
                } else {
                    String msg = "Ssl handshake failed";
                    log.info(msg, future.cause());
                    listener.onThrowable(msg, future.cause());
                }
            }
        });
    }

    @Override
    public void finishRequest() {
        if (shouldChannelBeKept(serverChannel)) {
            cancel(true);
            if (serverChannel.isActive()) {
                offerChannelToConnectionPool(serverChannel, connectionsPool);
            }
        } else {
            cancel(false);
        }
    }

    private static final List<Class> handlersToKeepInPool = Lists.<Class>newArrayList(SslHandler.class);

    private void offerChannelToConnectionPool(final Channel channel, final SimpleConnectionsPool connectionsPool) {
        // 免费代理不进入连接池, 因为免费代理的连接池策略无法确定
        // 免费代理有可能将当前域名的请求发到上一次请求的域名ip
        // 实际上这个问题可以通过 poolKey = target + proxy 解决(目前是 poolKey = proxy)
        // 但是因为这样会造成连接池连接数量剧增, 所以放弃这种策略
        /*if (connectionsPool == null || proxyServer.isFree() || proxyServer.isSocks()) {
            channel.close();
            return;
        }*/
        channel.eventLoop().submit(new Callable<Object>() { //必须在该channel的工作线程里完成，否则会有死锁
            @Override
            public Object call() throws Exception {
                NetworkUtil.removeAllFromPipelineExcept(channel.pipeline(), handlersToKeepInPool);
                channel.pipeline().addLast(new ExceptionCaughtHandler());
                if (!connectionsPool.offer(channel.attr(CONNECTION_POOL_KEY).get(), channel)) {
                    channel.close();
                } else {
                    log.debug("Connection [{}] add to pool successfully", channel.attr(CONNECTION_POOL_KEY).get());
                }
                return null;
            }
        });
    }

    public static boolean shouldChannelBeKept(Channel channel) {
        Boolean keepAlive = channel.attr(KEEP_ALIVE).get();
        return keepAlive != null && keepAlive;
    }

    @Override
    public void sendRequest(FullHttpRequest request, boolean https) {
        if (listener.isCompleted()) {
            log.warn("Request has been completed");
            cancel(false);
        } else {
            if (https) {
                sendHttpsCONNECT(request);
            } else {
                sendRawRequest(request);
            }
        }
    }

    protected void setServerChannel(NioSocketChannel serverChannel) {
        this.serverChannel = serverChannel;
    }

    public boolean isHttps() {
        return isHttps;
    }
}
