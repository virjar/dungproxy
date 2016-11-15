package com.virjar.client.http;

/**
 * @author lingtong.fu
 * @version 2016-09-04 22:49
 */

import com.google.common.collect.ImmutableMap;
import com.ning.http.client.ProxyServer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class HttpOption {

    public static final int NOTSET = -1;
    // http 请求头
    private Map<String, String> headers;
    private ProxyServer proxy;
    // post 的表单数据
    private Map<String, String> postFormData;
    // post 的 body 数据
    // 注: body 和 form 互斥, 同时存在时默认使用 body
    private String postBodyData;

    private int requestTimeoutInMs = NOTSET;

    public synchronized HttpOption addHeader(String key, String value) {
        if (headers == null) headers = new HashMap<>();
        headers.put(key, value);
        return this;
    }

    public HttpOption setProxy(String host, int port) {
        proxy = new ProxyServer(host, port);
        return this;
    }

    public synchronized Map<String, String> getHeaders() {
        if (headers == null) return Collections.emptyMap();
        return ImmutableMap.copyOf(headers);
    }

    public ProxyServer getProxy() {
        return this.proxy;
    }

    public HttpOption addPostFormData(String key, String value) {
        if (postFormData == null) postFormData = new HashMap<>();
        postFormData.put(key, value);
        return this;
    }

    public Map<String, String> getPostFormData() {
        if (postFormData == null) return Collections.emptyMap();
        return ImmutableMap.copyOf(postFormData);
    }

    public String getPostBodyData() {
        return postBodyData;
    }

    public HttpOption setPostBodyData(String postBodyData) {
        this.postBodyData = postBodyData;
        return this;
    }

    public HttpOption withRequestTimeoutInMs(int requestTimeoutInMs) {
        this.requestTimeoutInMs = requestTimeoutInMs;
        return this;
    }

    public int getRequestTimeoutInMs() {
        return this.requestTimeoutInMs;
    }
}
