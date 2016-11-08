package com.virjar.dungproxy.server.proxyservice.client;

import com.virjar.dungproxy.server.entity.Proxy;
import com.virjar.dungproxy.server.model.ProxyModel;
import com.virjar.dungproxy.server.proxyservice.client.listener.DefaultRequestExecutor;
import com.virjar.dungproxy.server.proxyservice.client.listener.RequestExecutorProxy;
import com.virjar.dungproxy.server.proxyservice.client.listener.ResponseListener;
import com.virjar.dungproxy.server.proxyservice.client.listener.SimpleConnectionsPool;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.ssl.SslContext;

import java.io.Closeable;
import java.io.IOException;


public class SimpleHttpClient implements Closeable {

    private int defaultReadTimeoutMs = 5000;
    private int defaultRequestTimeoutMs = 15000;
    private int defaultConnectionTimeoutMs = 2000;
    private int defaultWriteBufferLowWaterMark = 262144;
    private int defaultWriteBufferHighWaterMark = 524288;
    private EventLoopGroup defaultExecutor;
    private SslContext sslContext;
    private SimpleConnectionsPool connectionsPool;

    public SimpleHttpClient(int readTimeoutMs, int requestTimeoutMs, int connectionTimeoutMs, int writeBufferLowWaterMark, int writeBufferHighWaterMark, SslContext sslContext, NioEventLoopGroup defaultExecutor, SimpleConnectionsPool connectionsPool) {
        this.defaultReadTimeoutMs = readTimeoutMs;
        this.defaultRequestTimeoutMs = requestTimeoutMs;
        this.defaultConnectionTimeoutMs = connectionTimeoutMs;
        this.defaultWriteBufferLowWaterMark = writeBufferLowWaterMark;
        this.defaultWriteBufferHighWaterMark = writeBufferHighWaterMark;
        this.sslContext = sslContext;
        this.connectionsPool = connectionsPool;
        this.defaultExecutor = defaultExecutor;
    }

    public RequestBuilder prepare(
            FullHttpRequest request,
            ResponseListener listener,
            Proxy proxyServer
    ) {
        return new RequestBuilder(request, listener, proxyServer, connectionsPool);
    }

    @Override
    public void close() throws IOException {
        defaultExecutor.shutdownGracefully().awaitUninterruptibly();
        connectionsPool.destroy();
    }

    public class RequestBuilder {
        private FullHttpRequest request;
        private Proxy proxyServer;
        private ResponseListener listener;
        private EventLoopGroup executor = defaultExecutor;
        private int readTimeoutMs = defaultReadTimeoutMs;
        private int requestTimeoutMs = defaultRequestTimeoutMs;
        private int connectionTimeoutMs = defaultConnectionTimeoutMs;
        private int writeBufferLowWaterMark = defaultWriteBufferLowWaterMark;
        private int writeBufferHighWaterMark = defaultWriteBufferHighWaterMark;
        private SimpleConnectionsPool connectionsPool;
        private boolean customAuth = false;
        private boolean retry;

        public RequestBuilder(FullHttpRequest request, ResponseListener listener, Proxy proxyServer, SimpleConnectionsPool connectionsPool) {
            this.request = request;
            this.listener = listener;
            this.proxyServer = proxyServer;
            this.connectionsPool = connectionsPool;
        }

        public void setReadTimeoutMs(int readTimeoutMs) {
            this.readTimeoutMs = readTimeoutMs;
        }

        public void setRequestTimeoutMs(int requestTimeoutMs) {
            this.requestTimeoutMs = requestTimeoutMs;
        }

        public void setConnectionTimeoutMs(int connectionTimeoutMs) {
            this.connectionTimeoutMs = connectionTimeoutMs;
        }

        public void setExecutor(EventLoopGroup executor) {
            this.executor = executor;
        }

        public void setWriteBufferLowWaterMark(int writeBufferLowWaterMark) {
            this.writeBufferLowWaterMark = writeBufferLowWaterMark;
        }

        public void setWriteBufferHighWaterMark(int writeBufferHighWaterMark) {
            this.writeBufferHighWaterMark = writeBufferHighWaterMark;
        }

        public void setConnectionsPool(SimpleConnectionsPool connectionsPool) {
            this.connectionsPool = connectionsPool;
        }

        public void setCustomAuth(boolean customAuth) {
            this.customAuth = customAuth;
        }

        public void setRetry(boolean retry) {
            this.retry = retry;
        }

        public RequestExecutorProxy execute() {
            return new DefaultRequestExecutor(
                    readTimeoutMs,
                    requestTimeoutMs,
                    connectionTimeoutMs,
                    writeBufferLowWaterMark,
                    writeBufferHighWaterMark,
                    executor,
                    request,
                    listener,
                    sslContext,
                    proxyServer,
                    connectionsPool,
                    customAuth,
                    retry
            ).execute();
        }
    }
}
