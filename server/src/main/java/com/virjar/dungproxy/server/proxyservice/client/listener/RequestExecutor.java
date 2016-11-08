package com.virjar.dungproxy.server.proxyservice.client.listener;

import com.virjar.dungproxy.server.entity.Proxy;
import io.netty.handler.codec.http.FullHttpRequest;


/**
 * This interface is designed for one-time use
 */
public interface RequestExecutor {

    /**
     * Execute a http request
     */
    RequestExecutorProxy execute();

    /**
     * Cancel current request
     */
    void cancel(boolean keepServerChannel);

    void finishRequest();

    Proxy getProxyServer();

    void sendRequest(FullHttpRequest httpRequest, boolean https);
}
