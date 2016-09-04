package com.virjar.http;

/**
 * 存储请求参数的类, 包括但不仅限于
 * headers : http 请求头
 * proxy : 代理
 * postFormData : post 的表单数据
 * postBodyData : post 的 body 数据, 之一 body 和 form 二者只能存在其一, 同时存在时使用 body
 *
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

    //<key,value>
    private Map<String, String> headers;
    //<host,port>
    private ProxyServer proxy;
    private Map<String, String> postFormData;
    private String postBodyData;

    private int requestTimeoutInMs = NOTSET;

    public synchronized HttpOption addHeader(String key, String value) {
        if (headers == null) headers = new HashMap<String, String>();
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
        if (postFormData == null) postFormData = new HashMap<String, String>();
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
