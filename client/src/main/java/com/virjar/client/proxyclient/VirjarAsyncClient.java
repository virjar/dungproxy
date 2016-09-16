package com.virjar.client.proxyclient;

import com.google.common.base.Strings;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.AsyncCompletionHandlerBase;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ProxyServer;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import com.ning.http.client.multipart.StringPart;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;

import com.virjar.client.concurrent.ManagedExecutors;
import com.virjar.client.concurrent.NamedThreadFactory;
import com.virjar.client.http.AsyncClientHandler;
import com.virjar.client.http.GuavaListenableFuture;
import com.virjar.client.http.HttpOption;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//jdk1.7没有这个方法,编译不通过
//import static com.sun.deploy.Environment.setUserAgent;

import com.ning.http.client.AsyncHttpClientConfig.Builder;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

/**
 * Description: AsyncClient
 *
 * @author lingtong.fu
 * @version 2016-09-04 11:24
 */
public class VirjarAsyncClient {
    static {
        check();
    }

    public VirjarAsyncClient() {
        shareExecutor = true;

        setCompressionEnabled(true);

        // pooling conf
        setAllowPoolingConnection(true);
        setIdleConnectionInPoolTimeoutInMs(60000);

        // connection conf
        setMaximumConnectionsTotal(100);
        setMaximumConnectionsPerHost(20);

        // request conf
        setConnectionTimeoutInMs(1000);
        setRequestTimeoutInMs(60000);
       // setUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) Chrome/27.0.1453.94 Safari/537.36 hc/8.0.1");

    }

    @PreDestroy
    public void destroy() {
        if (getClient() != null)
            getClient().close();
    }

    <T> ListenableFuture<T> privateGet(String url, HttpOption option, AsyncHandler<T> handler) throws IOException {
        AsyncHttpClient.BoundRequestBuilder builder = getClient().prepareGet(url);

        if (option != null) {
            //设置header
            for (Map.Entry<String, String> entry : option.getHeaders().entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }

            //设置代理
            if (option.getProxy() != null) {
                builder.setProxyServer(option.getProxy());
            }

            if (option.getRequestTimeoutInMs() != HttpOption.NOTSET) {
                builder.setRequestTimeout(option.getRequestTimeoutInMs());
            }

        }
        Request request = builder.build();
        // 这个 IOException 异常捕获不到了, 等待 ahc 升级解决这个问题吧

        return new GuavaListenableFuture<T>(getClient().executeRequest(request, new AsyncClientHandler<T>(handler)));
    }

    <T> ListenableFuture<T> privatePost(final String url, HttpOption option, AsyncHandler<T> handler) throws IOException {
        AsyncHttpClient.BoundRequestBuilder builder = getClient().preparePost(url);
        if (option != null) {
            Map<String, String> postFormData = option.getPostFormData();
            Map<String, String> headers = option.getHeaders();
            String body = option.getPostBodyData();
            ProxyServer proxy = option.getProxy();

            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    builder.addHeader(entry.getKey(), entry.getValue());
                }
            }

            if (postFormData != null && !postFormData.isEmpty()) {
                for (Map.Entry<String, String> entry : postFormData.entrySet()) {
                    builder.addBodyPart(new StringPart(entry.getKey(), entry.getValue()));
                }
            }

            if (!Strings.isNullOrEmpty(body)) {
                builder.setBody(body);
            }

            if (proxy != null) {
                builder.setProxyServer(proxy);
            }
        }

        Request request = builder.build();
        return new GuavaListenableFuture<T>(getClient().executeRequest(request, new AsyncClientHandler<T>(handler)));
    }

    @Deprecated
    <T> ListenableFuture<T> privatePost(final String url, Map<String, String> params, HttpOption option, String charset, AsyncHandler<T> handler) throws IOException {
        AsyncHttpClient.BoundRequestBuilder builder = getClient().preparePost(url);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            StringPart part = new StringPart(entry.getKey(), entry.getValue(), null, Charset.forName(charset));
            builder.addBodyPart(part);
        }

        if (option != null) {
            //设置header
            for (Map.Entry<String, String> entry : option.getHeaders().entrySet()) {
                builder.addHeader(entry.getKey(), entry.getValue());
            }

            //设置代理
            if (option.getProxy() != null) {
                builder.setProxyServer(option.getProxy());
            }
        }

        Request request = builder.build();
        return new GuavaListenableFuture<T>(getClient().executeRequest(request, new AsyncClientHandler<T>(handler)));
    }

    /**
     * 建AsyncCompletionHandlerBase，想记录异常的可以写个子类重写onThrow方法
     */
    public <T> ListenableFuture<T> get(final String url, AsyncHandler<T> handler) throws IOException {
        return privateGet(url, null, handler);
    }


    public <T> ListenableFuture<T> get(final String url, HttpOption option, AsyncHandler<T> handler) throws IOException {
        return privateGet(url, option, handler);
    }

    public ListenableFuture<Response> get(final String url) throws IOException {
        return get(url, (HttpOption) null);
    }

    public ListenableFuture<Response> get(final String url, final HttpOption option) throws IOException {
        return privateGet(url, option, new AsyncCompletionHandlerBase());
    }

    /**
     * @param url
     * @return
     * @throws IOException
     * @see ListenableFuture<Response> get(final String url) throws IOException
     */
    @Deprecated
    public ListenableFuture<Response> getWithoutHandler(final String url) throws IOException {
        return get(url);
    }

    /**
     * @param url
     * @param option
     * @return
     * @throws IOException
     * @see ListenableFuture<Response> get(final String url, final QHttpOption option) throws IOException
     */
    @Deprecated
    public ListenableFuture<Response> getWithoutHandler(final String url, HttpOption option) throws IOException {
        return get(url, option);
    }

    //返回值见Futures.successfulAsList的注释
    public <T> ListenableFuture<List<T>> get(final Map<String, AsyncHandler<T>> urlAndHandlers) {
        ListenableFuture[] futures = new ListenableFuture[urlAndHandlers.size()];
        int index = 0;
        for (Map.Entry<String, AsyncHandler<T>> asyncHandlerEntry : urlAndHandlers.entrySet()) {
            String url = asyncHandlerEntry.getKey();
            AsyncHandler<T> handler = asyncHandlerEntry.getValue();
            try {
                futures[index] = get(url, handler);
            } catch (Exception e) {
                futures[index] = Futures.immediateFailedFuture(e);
                log.error("invoke url error,url is {}", url, e);
            }
            index++;
        }

        return Futures.successfulAsList(futures);
    }

    public ListenableFuture<List<Response>> get(List<String> urls) {
        Map<String, AsyncHandler<Response>> maps = Maps.newHashMapWithExpectedSize(urls.size());
        for (String url : urls) {
            maps.put(url, new AsyncCompletionHandlerBase());
        }
        return get(maps);
    }

    public <T> ListenableFuture<T> post(String url, HttpOption option, AsyncHandler<T> handler) throws IOException {
        return privatePost(url, option, handler);
    }

    public ListenableFuture<Response> post(String url, HttpOption option) throws IOException {
        return privatePost(url, option, new AsyncCompletionHandler<Response>() {
            @Override
            public Response onCompleted(Response response) throws Exception {
                return response;
            }
        });
    }

    public void setCompressionEnabled(boolean compressionEnabled) {
        builder.setCompressionEnforced(compressionEnabled);
    }

    public void setAllowPoolingConnection(boolean allowPoolingConnection) {
        builder.setAllowPoolingConnections(allowPoolingConnection);
    }

    public void setIdleConnectionInPoolTimeoutInMs(int defaultIdleConnectionInPoolTimeoutInMs) {
        builder.setPooledConnectionIdleTimeout(defaultIdleConnectionInPoolTimeoutInMs);
    }

    public void setMaximumConnectionsTotal(int defaultMaxTotalConnections) {
        builder.setMaxConnections(defaultMaxTotalConnections);
    }

    public void setMaximumConnectionsPerHost(int defaultMaxConnectionPerHost) {
        builder.setMaxConnectionsPerHost(defaultMaxConnectionPerHost);
    }

    public void setConnectionTimeoutInMs(int defaultConnectionTimeOutInMs) {
        builder.setConnectTimeout(defaultConnectionTimeOutInMs);
    }

    public void setRequestTimeoutInMs(int defaultRequestTimeoutInMs) {
        builder.setRequestTimeout(defaultRequestTimeoutInMs);
    }

    private static final Logger log = LoggerFactory.getLogger(VirjarAsyncClient.class);

    private static void check() {
        try {
            Class.forName("org.jboss.netty.bootstrap.ClientBootstrap");
        } catch (ClassNotFoundException e) {
            String message = "使用 VirjarAsyncClient 必须引入io.netty:netty all in one包.";
            log.error(message);
            throw new RuntimeException(message);
        }
    }

    private static final Supplier<HashedWheelTimer> TIMER = Suppliers.memoize(new Supplier<HashedWheelTimer>() {
        @Override
        public HashedWheelTimer get() {
            HashedWheelTimer timer = new HashedWheelTimer();
            timer.start();
            return timer;
        }
    });

    private static final Supplier<NioClientSocketChannelFactory> CHANNEL_FACTORY = Suppliers.memoize(new Supplier<NioClientSocketChannelFactory>() {
        @Override
        public NioClientSocketChannelFactory get() {
            return new NioClientSocketChannelFactory(ManagedExecutors.getExecutor(), ManagedExecutors.getExecutor(), Runtime.getRuntime().availableProcessors());
        }
    });

    private final Supplier<AsyncHttpClient> lazyClient = Suppliers.memoize(new Supplier<AsyncHttpClient>() {

        @Override
        public AsyncHttpClient get() {
            builder.setExecutorService(ManagedExecutors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2, fact));
            NettyAsyncHttpProviderConfig providerConfig = new NettyAsyncHttpProviderConfig();
            providerConfig.setNettyTimer(TIMER.get());
            if (shareExecutor) {
                providerConfig.setBossExecutorService(ManagedExecutors.getExecutor());
                providerConfig.setSocketChannelFactory(CHANNEL_FACTORY.get());
            }
            builder.setAsyncHttpClientProviderConfig(providerConfig);
            return new AsyncHttpClient(builder.build());
        }
    });

    private boolean shareExecutor;

    private final Builder builder = new Builder();

    private static final ThreadFactory fact = new NamedThreadFactory("hc-nio");

    public AsyncHttpClient getClient() {
        return lazyClient.get();
    }
}
