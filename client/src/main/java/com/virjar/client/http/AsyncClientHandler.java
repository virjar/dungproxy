package com.virjar.client.http;

import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHandlerExtensions;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.ProgressAsyncHandler;

import java.net.InetAddress;

/**
 * Description: AsyncClientHandler
 *
 * @author lingtong.fu
 * @version 2016-09-04 23:02
 */

public  class AsyncClientHandler<T> implements AsyncHandler<T>, AsyncHandlerExtensions, ProgressAsyncHandler<T> {
    private final AsyncHandler<T> proxy;

    private final AsyncHandlerExtensions asyncHandlerExtensions;
    private final ProgressAsyncHandler progressAsyncHandler;

    public AsyncClientHandler(AsyncHandler<T> proxy) {
        this.proxy = proxy;
        this.asyncHandlerExtensions = (proxy instanceof AsyncHandlerExtensions) ? AsyncHandlerExtensions.class.cast(proxy) : null;
        this.progressAsyncHandler = (proxy instanceof ProgressAsyncHandler) ? ProgressAsyncHandler.class.cast(proxy) : null;
    }

    @Override
    public void onThrowable(Throwable t) {
        try {
            proxy.onThrowable(t);
        } finally {
            //TODO log
            //scope.close();
        }
    }

    @Override
    public T onCompleted() throws Exception {
        try {
            return proxy.onCompleted();
        } finally {
            //TODO log
            //scope.close();
        }
    }

    @Override
    public STATE onBodyPartReceived(HttpResponseBodyPart content) throws Exception {
        return proxy.onBodyPartReceived(content);
    }

    @Override
    public STATE onStatusReceived(HttpResponseStatus status) throws Exception {
        return proxy.onStatusReceived(status);
    }

    @Override
    public STATE onHeadersReceived(HttpResponseHeaders headers) throws Exception {
        return proxy.onHeadersReceived(headers);
    }

    @Override
    public void onOpenConnection() {
        if (asyncHandlerExtensions != null) {
            asyncHandlerExtensions.onOpenConnection();
        }
    }

    @Override
    public void onConnectionOpen() {
        if (asyncHandlerExtensions != null) {
            asyncHandlerExtensions.onConnectionOpen();
        }
    }

    @Override
    public void onPoolConnection() {
        if (asyncHandlerExtensions != null) {
            asyncHandlerExtensions.onPoolConnection();
        }
    }

    @Override
    public void onConnectionPooled() {
        if (asyncHandlerExtensions != null) {
            asyncHandlerExtensions.onConnectionPooled();
        }
    }

    @Override
    public void onSendRequest(Object request) {
        if (asyncHandlerExtensions != null) {
            asyncHandlerExtensions.onSendRequest(request);
        }
    }

    @Override
    public void onRetry() {
        if (asyncHandlerExtensions != null) {
            asyncHandlerExtensions.onRetry();
        }
    }

    @Override
    public void onDnsResolved(InetAddress address) {
        if (asyncHandlerExtensions != null) {
            asyncHandlerExtensions.onDnsResolved(address);
        }
    }

    @Override
    public void onSslHandshakeCompleted() {
        if (asyncHandlerExtensions != null) {
            asyncHandlerExtensions.onSslHandshakeCompleted();
        }
    }

    @Override
    public STATE onHeaderWriteCompleted() {
        if (progressAsyncHandler != null) {
            return progressAsyncHandler.onHeaderWriteCompleted();
        }
        return STATE.CONTINUE;
    }

    @Override
    public STATE onContentWriteCompleted() {
        if (progressAsyncHandler != null) {
            return progressAsyncHandler.onContentWriteCompleted();
        }
        return STATE.CONTINUE;
    }

    @Override
    public STATE onContentWriteProgress(long amount, long current, long total) {
        if (progressAsyncHandler != null) {
            return progressAsyncHandler.onContentWriteProgress(amount, current, total);
        }
        return STATE.CONTINUE;
    }
}