package com.virjar.dungproxy.client.httpclient;

import java.io.IOException;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;

import com.virjar.dungproxy.client.ippool.config.ProxyConstant;
import com.virjar.dungproxy.client.model.AvProxy;

/**
 * Created by virjar on 16/10/30.
 * <br/>
 * 必须注册到httpclient里面,才能实现IP自动下线
 */
public class DunProxyHttpRequestRetryHandler implements HttpRequestRetryHandler {
    public static final HttpRequestRetryHandler INSTANCE = new DunProxyHttpRequestRetryHandler(new DefaultHttpRequestRetryHandler(3, true));
    private HttpRequestRetryHandler delegate;

    public DunProxyHttpRequestRetryHandler(HttpRequestRetryHandler delegate) {
        if (delegate == null) {
            delegate = INSTANCE;
        }
        if (delegate instanceof DunProxyHttpRequestRetryHandler) {
            DunProxyHttpRequestRetryHandler dung = (DunProxyHttpRequestRetryHandler) delegate;
            delegate = dung.delegate;
        }
        this.delegate = delegate;
    }

    @Override
    public boolean retryRequest(IOException exception, int executionCount, HttpContext context) {
        AvProxy proxy = (AvProxy) context.getAttribute(ProxyConstant.USED_PROXY_KEY);
        if (proxy != null) {
            proxy.recordFailed();
        }
        boolean ret = false;
        try {
            ret = delegate.retryRequest(exception, executionCount, context);
        } finally {
            if (ret && proxy != null) {
                proxy.recordUsage();
            }
        }
        return ret;
    }
}
