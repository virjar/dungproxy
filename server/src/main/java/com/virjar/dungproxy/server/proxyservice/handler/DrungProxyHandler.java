package com.virjar.dungproxy.server.proxyservice.handler;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.virjar.dungproxy.server.entity.DomainIp;
import com.virjar.dungproxy.server.entity.Proxy;
import com.virjar.dungproxy.server.proxyservice.client.SimpleHttpClient;
import com.virjar.dungproxy.server.proxyservice.client.exception.ServerChannelInactiveException;
import com.virjar.dungproxy.server.proxyservice.client.exception.ServerChannelNotWritableException;
import com.virjar.dungproxy.server.proxyservice.client.listener.AbstractResponseListener;
import com.virjar.dungproxy.server.proxyservice.client.listener.DefaultRequestExecutor;
import com.virjar.dungproxy.server.proxyservice.client.listener.RequestExecutor;
import com.virjar.dungproxy.server.proxyservice.client.listener.RequestExecutorProxy;
import com.virjar.dungproxy.server.proxyservice.common.ProxyResponse;
import com.virjar.dungproxy.server.proxyservice.common.util.NetworkUtil;
import com.virjar.dungproxy.server.proxyservice.server.ProxySelectorHolder;
import com.virjar.dungproxy.server.repository.DomainIpRepository;
import com.virjar.dungproxy.server.repository.ProxyRepository;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.querydsl.QPageRequest;

import javax.annotation.Resource;
import java.util.List;

import static com.virjar.dungproxy.server.proxyservice.common.AttributeKeys.DOMAIN;
import static com.virjar.dungproxy.server.proxyservice.common.AttributeKeys.PROXY_SELECTOR_HOLDER;
import static com.virjar.dungproxy.server.proxyservice.common.AttributeKeys.REQUEST_TIMEOUT;
import static com.virjar.dungproxy.server.proxyservice.common.AttributeKeys.SIMPLE_HTTP_CLIENT;
import static com.virjar.dungproxy.server.proxyservice.common.Constants.CONNECTION_RESET_MSG;
import static com.virjar.dungproxy.server.proxyservice.common.ProxyResponse.TOO_MANY_CONNECTION_RESPONSE;
import static com.virjar.dungproxy.server.proxyservice.common.ProxyResponse.proxyError;
import static com.virjar.dungproxy.server.proxyservice.common.util.NetworkUtil.isCodeValid;
import static io.netty.util.AttributeKey.valueOf;

/**
 * Description: DrungProxyHandler
 *
 * @author lingtong.fu
 * @version 2016-10-31 04:27
 */
public class DrungProxyHandler extends EndpointHandler {

    private static final Logger log = LoggerFactory.getLogger(DrungProxyHandler.class);

    private static final AttributeKey<Boolean> CUSTOM_USER_AGENT = valueOf("cusUserAgent");

    private Channel clientChannel;
    private String clientIp;

    private FullHttpRequest request;
    private int protocol;
    private String channelHex;
    private AbstractResponseListener listener;
    private RequestExecutorProxy requestExecutor;
    private Proxy proxy;
    private SimpleHttpClient proxyClient;
    private String domain;
    private ProxySelectorHolder proxySelectorHolder;


    /**
     * Time
     */
    private long perRequestStart;
    private long totalRequestStart;
    private int requestTimeout;

    /**
     * 请求策略参数
     */
    private int respCode;
    private int connectRetryCnt = 0;
    private int requestRetryCnt = 0;
    private static final int MAX_CONNECT_RETRY_CNT = 10;
    private static final int MAX_REQUEST_RETRY_CNT = 5;

    /**
     * 流量
     */
    private long traffic;
    private boolean hasWriteback = false;

    /**
     * 请求结果
     */
    private String content;


    public DrungProxyHandler(Channel clientChannel, String clientIp) {

        this.clientChannel = clientChannel;
        this.clientIp = clientIp;
        this.channelHex = Integer.toHexString(this.clientChannel.hashCode());
        this.requestTimeout = NetworkUtil.getAttr(clientChannel, REQUEST_TIMEOUT);
        this.domain = NetworkUtil.getAttr(clientChannel, DOMAIN);
        this.proxySelectorHolder = NetworkUtil.getAttr(clientChannel, PROXY_SELECTOR_HOLDER);
        this.proxyClient = NetworkUtil.getAttr(clientChannel, SIMPLE_HTTP_CLIENT);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Preconditions.checkArgument(msg instanceof FullHttpRequest);
        Boolean customUserAgent = ctx.channel().attr(CUSTOM_USER_AGENT).get();
        try {
            proxy = proxySelectorHolder.selectProxySelector(domain);
            request = (FullHttpRequest) msg;
            protocol = NetworkUtil.isSchemaHttps(request.uri()) ? 1 : 0;
            perRequestStart = totalRequestStart = System.currentTimeMillis();

            sendRequest(ctx, request.headers().contains(HttpHeaderNames.AUTHORIZATION), customUserAgent != null && customUserAgent, true);
        } catch (Exception e) {
            log.error("[FAILED] [] 请求失败, 获取代理失败, 域名:{} 异常: ", domain, e);
            ctx.channel().close();
        }
    }

    private void sendRequest(final ChannelHandlerContext ctx, final boolean customAuth, final boolean customUserAgent, final boolean retry) {
        if (request.refCnt() <= 0) { //可能由于客户端的结束，request此时已经释放
            return;
        }

        listener = new AbstractResponseListener() {

            private String respStr;
            private int writeCnt;
            private int flushCnt;
            private long connectCompletedTime;
            private long handShakeSuccTime;
            private long headerReceivedTime;
            private long requestSentTime;
            private boolean useCachedConn;

            private String getTimeLine() {
                long sysTime = System.currentTimeMillis();
                StringBuilder sb = new StringBuilder();
                if (connectCompletedTime > 0) {
                    sb.append(connectCompletedTime - perRequestStart);
                    sb.append(" ");
                    sb.append(useCachedConn);
                    sb.append(" CONN/");
                }
                if (handShakeSuccTime > 0) {
                    sb.append(handShakeSuccTime - connectCompletedTime);
                    sb.append(" HanS/");
                }
                if (requestSentTime > 0) {
                    if (handShakeSuccTime > 0) {
                        sb.append(requestSentTime - handShakeSuccTime);
                    } else {
                        sb.append(requestSentTime - connectCompletedTime);
                    }
                    sb.append(" ReqS/");
                }
                if (headerReceivedTime > 0) {
                    sb.append(headerReceivedTime - requestSentTime);
                    sb.append(" HeadR/");
                    sb.append(sysTime - headerReceivedTime);
                    sb.append(" DataR/");
                }
                sb.append(sysTime - perRequestStart);
                sb.append(" PerReq/");
                sb.append(sysTime - totalRequestStart);
                sb.append(" ReqT");
                return sb.toString();
            }

            @Override
            public State doOnConnectCompleted(Result result) {
                this.connectCompletedTime = System.currentTimeMillis();
                Object attr = result.getAttr();
                this.useCachedConn = (attr == null) ? false : (Boolean) attr;
                if (result.isSuccess()) {
                    return State.CONTINUE;
                } else {
                    log.info("[PROCESS] [] 连接失败 [{}/{}] [{}] [{}] ms", result.getCause().getClass().getName(), result.getCause().getMessage(), connectRetryCnt, getTimeLine());
                    // 重试次数达到极限, 断开与客户端连接
                    if (++connectRetryCnt > MAX_CONNECT_RETRY_CNT) {
                        if (compareAndSetCompleted(false, true)) {
                            //连接超时
                            respCode = -200;
                            log.info("[FAIL] 代理连接失败次数达到极限, 暂无可用代理 [{}] ms", getTimeLine());
                            NetworkUtil.writeAndFlushAndClose(clientChannel, ProxyResponse.noAvailableProxy());
                        }
                    } else {
                        respCode = -200;
                        if (!isCompleted()) {
                            sendRequest(ctx, customAuth, true, true);
                        }
                    }
                    return State.ABORT;
                }
            }

            @Override
            public boolean doOnHeaderReceived(HttpResponse response) {
                respCode = response.status().code();
                this.respStr = response.status().reasonPhrase();
                this.headerReceivedTime = System.currentTimeMillis();

                HttpHeaders headers = response.headers();

                if (needRetry(respCode, headers)) {
                    if (isBlankPage(headers)) {
                        //空白页
                        respCode = -300;
                    }
                    boolean retry = requestRetryCnt++ < MAX_REQUEST_RETRY_CNT;
                    String msg = "[PROCESS] [{}] CODE [{}] 收到异常 Header [{}] [{}] [{}] [{}] ms";
                    if (retry) msg += " 需要重试";
                    log.info(msg, respCode, response.status().code(), response.status().reasonPhrase(), response.headers().entries(), getTimeLine());

                    if (retry) {
                        logRequestFailed(false);
                        requestExecutor.cancel();
                        if (!isCompleted()) {
                            sendRequest(ctx, customAuth, customUserAgent, requestRetryCnt < MAX_REQUEST_RETRY_CNT);
                        }
                    }
                    return retry;
                } else {
                    log.info(
                            "[PROCESS] [] 收到 Header [{}] [{}] [{}] [{}] ms",
                            response.status().code(),
                            response.status().reasonPhrase(),
                            response.headers().entries(),
                            getTimeLine()
                    );
                    return false;
                }
            }

            @Override
            public void doOnDataReceived(ByteBuf data) {
                NetworkUtil.releaseMsg(request);
                writeCnt++;
                int readableBytes = data.readableBytes();
                traffic += readableBytes;
                hasWriteback = true;

                if (!clientChannel.isActive()) {
                    NetworkUtil.releaseMsg(data);
                    clientChannel.flush();
                    if (compareAndSetCompleted(false, true)) {
                        log.info("[FAILED] [] 请求失败, 客户端已断开, [w/f {}/{}] [{}/{}] [{}] ms", writeCnt, flushCnt, respCode, respStr, getTimeLine());
                        executor.cancel(false);
                        //客户端断开连接.
                        respCode = -500;
                        logRequestFailed(true);
                    }
                } else if (!clientChannel.isWritable()) {
                    NetworkUtil.releaseMsg(data);
                    clientChannel.flush();
                    if (compareAndSetCompleted(false, true)) {
                        log.info("[FAILED] [] 请求失败, 客户端不可写, [w/f {}/{}] [{}/{}] [{}] ms", writeCnt, flushCnt, respCode, respStr, getTimeLine());
                        executor.cancel(false);
                        //客户端数据堆积过多, 不可写
                        respCode = -501;
                        logRequestFailed(true);
                    }
                } else {
                    clientChannel.write(data);
                }
            }

            @Override
            protected void doOnDataFlush(final ChannelHandlerContext serverContext, final boolean isLast) {
                NetworkUtil.releaseMsg(request);
                flushCnt++;
                clientChannel.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            if (isLast) {
                                if (requestRetryCnt > 0) {
                                    log.info("The request through the retry is successful ! ");
                                }
                                if (isCodeValid(respCode)) {
                                    log.info("[SUCCESS] [] 请求成功 [w/f {}/{}] 状态码 [{}/{}] [Retry {}] [{}] ms", writeCnt, flushCnt, respCode, respStr, requestRetryCnt, getTimeLine());
                                    logRequestSuccess(DefaultRequestExecutor.shouldChannelBeKept(serverContext.channel()));
                                } else {
                                    log.info("[FAIL] [] 请求失败, 状态码 [{}/{}] [Retry {}] [{}] ms", respCode, respStr, requestRetryCnt, getTimeLine());
                                }
                            } else {
                                // 自动 read
                                if (!serverContext.channel().config().isAutoRead()) {
                                    serverContext.channel().read();
                                }
                            }
                        } else {
                            executor.cancel(false);
                            log.info("[FAILED] [] 请求失败, 客户端回写失败 [{}] ms", getTimeLine(), future.cause());
                            // 客户端回写失败
                            respCode = -502;
                            logRequestFailed(true);
                        }
                    }
                });
            }

            @Override
            public void doOnRequestTimeout() {
                respCode = -400;
                NetworkUtil.releaseMsg(request);
                log.info("[FAIL] [] 超时 [{}/{}] ms", getTimeLine(), System.currentTimeMillis() - requestSentTime);
                logRequestFailed(true);
                NetworkUtil.writeAndFlushAndClose(clientChannel, ProxyResponse.proxyTimeout(proxy.getId()));
            }

            @Override
            public void doOnConnectionPoolIsFull() {
                NetworkUtil.releaseMsg(request);
                NetworkUtil.writeAndFlushAndClose(clientChannel, TOO_MANY_CONNECTION_RESPONSE);
            }

            @Override
            public void doOnThrowable(String errorTrace, Throwable cause) {
                clientChannel.flush();
                respCode = getExceptionCode(cause);
                if (respCode == -1000) {
                    log.error("[FAIL] [] 发生异常 Trace [{}] [{}] ms", errorTrace, getTimeLine(), cause);
                } else {
                    log.error("[FAIL] [] 发生异常 Trace [{}] [{}] ms [{}]", errorTrace, getTimeLine(), cause.getMessage());
                }
                if ((requestRetryCnt++ < MAX_REQUEST_RETRY_CNT) && !hasWriteback) {

                    log.warn("[PROCESS] 重试请求 []");
                    logRequestFailed(false);
                    sendRequest(ctx, customAuth, true, requestRetryCnt < MAX_REQUEST_RETRY_CNT);
                } else {
                    logRequestFailed(true);
                    NetworkUtil.releaseMsg(request);
                    writeFailedResponse("Unknown ex " + cause.getClass().getSimpleName());
                }
            }

            @Override
            protected void doOnHandshakeSuccess() {
                this.handShakeSuccTime = System.currentTimeMillis();
            }

            @Override
            protected void doOnRequestSent() {
                this.requestSentTime = System.currentTimeMillis();
            }

            @Override
            public void setRequestExecutor(RequestExecutor executor) {
                this.executor = executor;
            }

            @Override
            public boolean compareAndSetCompleted(boolean expect, boolean update) {
                NetworkUtil.releaseMsgCompletely(request);
                NetworkUtil.removeHandler(clientChannel.pipeline(), DrungProxyHandler.class);
                return super.compareAndSetCompleted(expect, update);
            }
        };

        log.info("[START] [] 请求开始");
        if (request.refCnt() <= 0) {
            writeFailedResponse("Request Released");
            return;
        }
        SimpleHttpClient httpClient = proxyClient;
        if (!customUserAgent) {
            request.headers().set(HttpHeaderNames.USER_AGENT, "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.111 Safari/537.36");
        }
        SimpleHttpClient.RequestBuilder builder = httpClient.prepare(request, listener, proxy);

        int minus = (int) (System.currentTimeMillis() - totalRequestStart);
        if (minus < 0) {
            minus = 0;
        }
        int timeout = requestTimeout - minus;
        if (timeout < 0) {
            timeout = requestTimeout;
        }
        builder.setRequestTimeoutMs(timeout);
        builder.setExecutor(clientChannel.eventLoop());
        builder.setCustomAuth(customAuth);
        builder.setRetry(retry);
        requestExecutor = builder.execute();
    }

    private static boolean needRetry(int respCode, HttpHeaders headers) {
        return ((respCode == 200 && isBlankPage(headers)));
    }

    public static boolean isBlankPage(HttpHeaders headers) {
        String ret = headers.get(HttpHeaderNames.CONTENT_LENGTH);
        return ret != null && ret.equals("0");
    }

    private void logRequestFailed(boolean finished) {
        long now = System.currentTimeMillis();
        log.info("此次请求失败, 耗时:{} s", now - totalRequestStart);
        if (finished) clientChannel.close();
    }

    private void logRequestSuccess(boolean keepAlive) {
        long now = System.currentTimeMillis();
        log.info("请求成功, 耗时:{} s", now - totalRequestStart);
        if (!keepAlive) {
            clientChannel.close();
        }
    }

    private int getExceptionCode(Throwable cause) {
        if (cause == null) {
            //未知异常
            return -1000;
        }

        if (cause instanceof ServerChannelInactiveException) {
            //服务器端断开连接.
            return -600;
        }

        if (cause instanceof ServerChannelNotWritableException) {
            //服务端数据堆积过多, 不可写
            return -601;
        }

        String msg = cause.getMessage();
        if (msg == null) {
            return -1000;
        }
        //连接重置
        return cause.getMessage().equals(CONNECTION_RESET_MSG) ? -1001 : -1000;
    }

    private void writeFailedResponse(String message) {
        log.error("Error occured msg : {} {}", message, channelHex);
        NetworkUtil.writeAndFlushAndClose(clientChannel, proxyError(proxy != null ? proxy.getId() : 0, message, channelHex));
    }

}
