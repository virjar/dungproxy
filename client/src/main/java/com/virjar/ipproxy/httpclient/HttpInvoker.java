package com.virjar.ipproxy.httpclient;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;

import com.virjar.ipproxy.ippool.config.ProxyConstant;

/**
 * 以静态方式封装httpclient,方便的http请求客户端<br/>
 * Created by virjar on 16/10/4.
 */
public class HttpInvoker {
    private static CrawlerHttpClient crawlerHttpClient;
    static {// TODO 是否考虑cookie reject
        SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(true).setSoLinger(-1).setSoReuseAddress(false)
                .setSoTimeout(ProxyConstant.SOCKETSO_TIMEOUT).setTcpNoDelay(true).build();
        crawlerHttpClient = CrawlerHttpClientBuilder.create().setMaxConnTotal(400).setMaxConnPerRoute(50)
                .setDefaultSocketConfig(socketConfig).build();
    }

    public static String get(String url, List<NameValuePair> nameValuePairs, Header[] headers, String proxyIp,
            int proxyPort) throws IOException {
        return crawlerHttpClient.get(url, nameValuePairs, headers, proxyIp, proxyPort);
    }

    public static String get(String url, List<NameValuePair> params, Charset charset, Header[] headers)
            throws IOException {
        return crawlerHttpClient.get(url, params, charset, headers);
    }

    public static String post(String url, String entity) throws IOException {
        return crawlerHttpClient.post(url, entity);
    }

    public static String get(String url, Charset charset, Header[] headers) throws IOException {
        return crawlerHttpClient.get(url, charset, headers);
    }

    public static String get(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) throws IOException {
        return crawlerHttpClient.get(url, params, charset, headers, proxyIp, proxyPort);
    }

    public static String get(String url) throws IOException {
        return crawlerHttpClient.get(url);
    }

    public static String getQuiet(String url) {
        return crawlerHttpClient.getQuiet(url);
    }

    public static String post(String url, Map<String, String> params) throws IOException {
        return crawlerHttpClient.post(url, params);
    }

    public static String get(String url, Map<String, String> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) throws IOException {
        return crawlerHttpClient.get(url, params, charset, headers, proxyIp, proxyPort);
    }

    public static String post(String url, List<NameValuePair> params) throws IOException {
        return crawlerHttpClient.post(url, params);
    }

    public static String get(String url, List<NameValuePair> params, String proxyIp, int proxyPort) throws IOException {
        return crawlerHttpClient.get(url, params, proxyIp, proxyPort);
    }

    public static String post(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) throws IOException {
        return crawlerHttpClient.post(url, params, charset, headers, proxyIp, proxyPort);
    }

    public static String get(String url, List<NameValuePair> params) throws IOException {
        return crawlerHttpClient.get(url, params);
    }

    public static String post(String url, String entity, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) throws IOException {
        return crawlerHttpClient.post(url, entity, charset, headers, proxyIp, proxyPort);
    }

    public static String get(String url, List<NameValuePair> params, Charset charset, String proxyIp, int proxyPort)
            throws IOException {
        return crawlerHttpClient.get(url, params, charset, proxyIp, proxyPort);
    }

    public static String get(String url, List<NameValuePair> params, Header[] headers) throws IOException {
        return crawlerHttpClient.get(url, params, headers);
    }

    public static String get(String url, Charset charset) throws IOException {
        return crawlerHttpClient.get(url, charset);
    }

    public static String get(String url, Header[] headers, String proxyIp, int proxyPort) throws IOException {
        return crawlerHttpClient.get(url, headers, proxyIp, proxyPort);
    }

    public static String post(String url, Object entity, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) throws IOException {
        return crawlerHttpClient.post(url, entity, charset, headers, proxyIp, proxyPort);
    }

    public static String get(String url, Header[] headers) throws IOException {
        return crawlerHttpClient.get(url, headers);
    }

    public static String get(String url, Charset charset, String proxyIp, int proxyPort) throws IOException {
        return crawlerHttpClient.get(url, charset, proxyIp, proxyPort);
    }

    public static String get(String url, List<NameValuePair> params, Charset charset) throws IOException {
        return crawlerHttpClient.get(url, params, charset);
    }

    public static String post(String url, Map<String, String> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) throws IOException {
        return crawlerHttpClient.post(url, params, charset, headers, proxyIp, proxyPort);
    }

    public static String get(String url, Charset charset, Header[] headers, String proxyIp, int proxyPort)
            throws IOException {
        return crawlerHttpClient.get(url, charset, headers, proxyIp, proxyPort);
    }

    public static String get(String url, String proxyIp, int proxyPort) throws IOException {
        return crawlerHttpClient.get(url, proxyIp, proxyPort);
    }

    public static String post(String url, Object entity) throws IOException {
        return crawlerHttpClient.post(url, entity);
    }

    public static String post(String url, HttpEntity entity, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) throws IOException {
        return crawlerHttpClient.post(url, entity, charset, headers, proxyIp, proxyPort);
    }

    public static String postQuiet(String url, Object object) {
        return crawlerHttpClient.postQuiet(url, object);
    }

    public static int getStatus(String url, String proxyIp, int proxyPort) throws IOException {
        return crawlerHttpClient.getStatus(url, proxyIp, proxyPort);
    }

    public static String getQuiet(String url, HttpClientContext httpClientContext) {
        return crawlerHttpClient.getQuiet(url, httpClientContext);
    }

    public static String get(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort, HttpClientContext httpClientContext) throws IOException {
        return crawlerHttpClient.get(url, params, charset, headers, proxyIp, proxyPort, httpClientContext);
    }

    public static String get(String url, HttpClientContext httpClientContext) throws IOException {
        return crawlerHttpClient.get(url, httpClientContext);
    }
}
