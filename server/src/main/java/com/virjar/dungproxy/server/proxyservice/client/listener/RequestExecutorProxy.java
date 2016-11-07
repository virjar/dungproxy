package com.virjar.dungproxy.server.proxyservice.client.listener;

/**
 * Proxy class for {@link RequestExecutor}
 */
public class RequestExecutorProxy {

    private RequestExecutor proxy;

    public RequestExecutorProxy(RequestExecutor proxy) {
        this.proxy = proxy;
    }

    public void cancel() {
        proxy.cancel(false);
    }
}
