package com.virjar.dungproxy.client.httpclient.execchain;

import java.io.IOException;
import java.util.Date;

import org.apache.http.HttpException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpExecutionAware;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.execchain.ClientExecChain;

/**
 * Created by virjar on 16/11/30.
 */
public class AutoClearCookieExec implements ClientExecChain {
    private ClientExecChain delegate;

    public AutoClearCookieExec(ClientExecChain delegate) {
        this.delegate = delegate;
    }

    @Override
    public CloseableHttpResponse execute(HttpRoute route, HttpRequestWrapper request, HttpClientContext clientContext,
            HttpExecutionAware execAware) throws IOException, HttpException {
        CloseableHttpResponse httpResponse = delegate.execute(route, request, clientContext, execAware);
        clientContext.getCookieStore().clearExpired(new Date(System.currentTimeMillis() - 1800000));//cookie只能保留半个小时
        return httpResponse;
    }
}
