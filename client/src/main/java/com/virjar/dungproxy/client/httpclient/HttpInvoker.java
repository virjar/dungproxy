package com.virjar.dungproxy.client.httpclient;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.LaxRedirectStrategy;

import com.virjar.dungproxy.client.httpclient.cookie.BarrierCookieStore;
import com.virjar.dungproxy.client.httpclient.cookie.CookieStoreGenerator;
import com.virjar.dungproxy.client.httpclient.cookie.MultiUserCookieStore;
import com.virjar.dungproxy.client.ippool.config.ProxyConstant;

/**
 * 以静态方式封装httpclient,方便的http请求客户端<br/>
 * Created by virjar on 16/10/4.
 */
public class HttpInvoker {
    private static CrawlerHttpClient crawlerHttpClient;
    static {
        crawlerHttpClient = buildDefault();
    }

    public static CrawlerHttpClient buildDefault() {
        SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(true).setSoLinger(-1).setSoReuseAddress(false)
                .setSoTimeout(ProxyConstant.SOCKETSO_TIMEOUT).setTcpNoDelay(true).build();

        return CrawlerHttpClientBuilder.create().setMaxConnTotal(1000).setMaxConnPerRoute(50)
                .setDefaultSocketConfig(socketConfig)
                // .setSSLSocketFactory(sslConnectionSocketFactory) 证书忽略逻辑转移到httpclient builder里面
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setDefaultCookieStore(new MultiUserCookieStore(new CookieStoreGenerator() {
                    @Override
                    public CookieStore generate(String user) {
                        return new BarrierCookieStore();
                    }
                })).build();
    }

    public static void setCrawlerHttpClient(CrawlerHttpClient crawlerHttpClient) {
        HttpInvoker.crawlerHttpClient = crawlerHttpClient;
    }

    public static String postJSON(String url, Object entity, Header[] headers) {
        return crawlerHttpClient.postJSON(url, entity, headers);
    }

    public static String get(String url) {
        return crawlerHttpClient.get(url);
    }

    public static String get(String url, Charset charset) {
        return crawlerHttpClient.get(url, charset);
    }

    public static String get(String url, Charset charset, Header[] headers) {
        return crawlerHttpClient.get(url, charset, headers);
    }

    public static String get(String url, Charset charset, Header[] headers, String proxyIp, int proxyPort) {
        return crawlerHttpClient.get(url, charset, headers, proxyIp, proxyPort);
    }

    public static String get(String url, Charset charset, String proxyIp, int proxyPort) {
        return crawlerHttpClient.get(url, charset, proxyIp, proxyPort);
    }

    public static String get(String url, Header[] headers) {
        return crawlerHttpClient.get(url, headers);
    }

    public static String get(String url, Header[] headers, HttpClientContext httpClientContext) {
        return crawlerHttpClient.get(url, headers, httpClientContext);
    }

    public static String get(String url, Header[] headers, String proxyIp, int proxyPort) {
        return crawlerHttpClient.get(url, headers, proxyIp, proxyPort);
    }

    public static String get(String url, HttpClientContext httpClientContext) {
        return crawlerHttpClient.get(url, httpClientContext);
    }

    public static String get(String url, List<NameValuePair> nameValuePairs, Header[] headers, String proxyIp,
            int proxyPort) {
        return crawlerHttpClient.get(url, nameValuePairs, headers, proxyIp, proxyPort);
    }

    public static String get(String url, List<NameValuePair> params) {
        return crawlerHttpClient.get(url, params);
    }

    public static String get(String url, List<NameValuePair> params, Charset charset) {
        return crawlerHttpClient.get(url, params, charset);
    }

    public static String get(String url, List<NameValuePair> params, Charset charset, Header[] headers) {
        return crawlerHttpClient.get(url, params, charset, headers);
    }

    public static String get(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return crawlerHttpClient.get(url, params, charset, headers, proxyIp, proxyPort);
    }

    public static String get(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort, HttpClientContext httpClientContext) {
        return crawlerHttpClient.get(url, params, charset, headers, proxyIp, proxyPort, httpClientContext);
    }

    public static String get(String url, List<NameValuePair> params, Charset charset, String proxyIp, int proxyPort) {
        return crawlerHttpClient.get(url, params, charset, proxyIp, proxyPort);
    }

    public static String get(String url, List<NameValuePair> params, Header[] headers) {
        return crawlerHttpClient.get(url, params, headers);
    }

    public static String get(String url, List<NameValuePair> params, String proxyIp, int proxyPort) {
        return crawlerHttpClient.get(url, params, proxyIp, proxyPort);
    }

    public static String get(String url, Map<String, String> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return crawlerHttpClient.get(url, params, charset, headers, proxyIp, proxyPort);
    }

    public static String get(String url, String proxyIp, int proxyPort) {
        return crawlerHttpClient.get(url, proxyIp, proxyPort);
    }

    public static int getStatus(String url, String proxyIp, int proxyPort) {
        return crawlerHttpClient.getStatus(url, proxyIp, proxyPort);
    }

    public static String post(String url, HttpEntity entity, Charset charset, Header[] headers, String proxyIp,
            int proxyPort, HttpClientContext httpClientContext) {
        return crawlerHttpClient.post(url, entity, charset, headers, proxyIp, proxyPort, httpClientContext);
    }

    public static String post(String url, String entity) {
        return crawlerHttpClient.post(url, entity);
    }

    public static String post(String url, String entity, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return crawlerHttpClient.post(url, entity, charset, headers, proxyIp, proxyPort);
    }

    public static String post(String url, String entity, Header[] headers) {
        return crawlerHttpClient.post(url, entity, headers);
    }

    public static String post(String url, List<NameValuePair> params) {
        return crawlerHttpClient.post(url, params);
    }

    public static String post(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return crawlerHttpClient.post(url, params, charset, headers, proxyIp, proxyPort);
    }

    public static String post(String url, List<NameValuePair> params, Header[] headers) {
        return crawlerHttpClient.post(url, params, headers);
    }

    public static String post(String url, Map<String, String> params) {
        return crawlerHttpClient.post(url, params);
    }

    public static String post(String url, Map<String, String> params, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return crawlerHttpClient.post(url, params, charset, headers, proxyIp, proxyPort);
    }

    public static String post(String url, Map<String, String> params, Header[] headers) {
        return crawlerHttpClient.post(url, params, headers);
    }

    public static String post(String url, List<NameValuePair> params, HttpClientContext httpClientContext) {
        return crawlerHttpClient.post(url, params, httpClientContext);
    }

    public static byte[] getEntity(String url) {
        return crawlerHttpClient.getEntity(url);
    }

    public static byte[] getEntity(String url, List<NameValuePair> params, Charset charset, Header[] headers,
            String proxyIp, int proxyPort, HttpClientContext httpClientContext) {
        return crawlerHttpClient.getEntity(url, params, charset, headers, proxyIp, proxyPort, httpClientContext);
    }

    public static String post(String url, Map<String, String> params, HttpClientContext httpClientContext) {
        return crawlerHttpClient.post(url, params, httpClientContext);
    }

    public static String postJSON(String url, Object entity) {
        return crawlerHttpClient.postJSON(url, entity);
    }

    public static String postJSON(String url, Object entity, HttpClientContext httpClientContext) {
        return crawlerHttpClient.postJSON(url, entity, httpClientContext);
    }

    public static String postJSON(String url, Object entity, Charset charset, Header[] headers, String proxyIp,
            int proxyPort) {
        return crawlerHttpClient.postJSON(url, entity, charset, headers, proxyIp, proxyPort);
    }

    public static String post(String url, Map<String, String> params, Header[] headers, String proxyIp, int proxyPort) {
        return crawlerHttpClient.post(url, params, headers, proxyIp, proxyPort);
    }

    public static String post(String url, List<NameValuePair> params, Header[] headers, String proxyIp, int proxyPort) {
        return crawlerHttpClient.post(url, params, headers, proxyIp, proxyPort);
    }

    public static String post(String url, List<NameValuePair> params, Header[] headers,
            HttpClientContext httpClientContext) {
        return crawlerHttpClient.post(url, params, headers, httpClientContext);
    }

    public static String get(String url, List<NameValuePair> params, Header[] headers,
            HttpClientContext httpClientContext) {
        return crawlerHttpClient.get(url, params, headers, httpClientContext);
    }

    public static byte[] getEntity(String url, Header[] headers) {
        return crawlerHttpClient.getEntity(url, headers);
    }
}
