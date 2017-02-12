package com.virjar.dungproxy.client.ningclient.conn;

import java.io.IOException;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.virjar.dungproxy.client.model.AvProxy;

/**
 * Created by virjar on 17/1/31.
 */
public class DungProxyHandler<T> implements AsyncHandler<T> {
    private AvProxy avProxy;// 绑定在这次请求的代理实例
    private AsyncHandler<T> delegate;

    public DungProxyHandler(AvProxy avProxy, AsyncHandler<T> delegate) {
        this.avProxy = avProxy;
        this.delegate = delegate;
    }

    @Override
    public void onThrowable(Throwable t) {
        if (t instanceof IOException) {
            avProxy.recordFailed();
        }
        delegate.onThrowable(t);
    }

    @Override
    public STATE onBodyPartReceived(HttpResponseBodyPart bodyPart) throws Exception {
        return delegate.onBodyPartReceived(bodyPart);
    }

    @Override
    public STATE onStatusReceived(HttpResponseStatus responseStatus) throws Exception {
        return delegate.onStatusReceived(responseStatus);
    }

    @Override
    public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
        return delegate.onHeadersReceived(headers);
    }

    @Override
    public T onCompleted() throws Exception {
        return delegate.onCompleted();
    }
}
