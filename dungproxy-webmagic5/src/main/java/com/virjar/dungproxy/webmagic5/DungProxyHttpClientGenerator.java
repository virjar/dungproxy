package com.virjar.dungproxy.webmagic5;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.client.CookieStore;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.HttpContext;

import com.virjar.dungproxy.client.httpclient.CrawlerHttpClient;
import com.virjar.dungproxy.client.httpclient.CrawlerHttpClientBuilder;
import com.virjar.dungproxy.client.httpclient.cookie.MultiUserCookieStore;

import us.codecraft.webmagic.Site;

/**
 * @author code4crafter@gmail.com <br>
 * @author virjar
 * @since 0.0.1
 */
public class DungProxyHttpClientGenerator {

    private PoolingHttpClientConnectionManager connectionManager;

    public DungProxyHttpClientGenerator() {
        // dungproxy再httpclient内部即处理https证书忽略问题
        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(100);
    }

    public DungProxyHttpClientGenerator setPoolSize(int poolSize) {
        connectionManager.setMaxTotal(poolSize);
        return this;
    }

    public CrawlerHttpClient getClient(Site site) {
        return generateClient(site);
    }

    private CrawlerHttpClient generateClient(Site site) {

        // 这里替换成我们的,才能方便的使用dungProxy的IP池
        CrawlerHttpClientBuilder httpClientBuilder = CrawlerHttpClientBuilder.create();

        httpClientBuilder.setConnectionManager(connectionManager);
        if (site.getUserAgent() != null) {
            httpClientBuilder.setUserAgent(site.getUserAgent());
        } // 这里和webMagic不同,如果没有设置UA,dungProxy会添加默认,是一个真实的浏览器UA
        if (site.isUseGzip()) {
            httpClientBuilder.addInterceptorFirst(new HttpRequestInterceptor() {

                public void process(final HttpRequest request, final HttpContext context)
                        throws HttpException, IOException {
                    if (!request.containsHeader("Accept-Encoding")) {
                        request.addHeader("Accept-Encoding", "gzip");
                    }
                }
            });
        }

        SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(site.getTimeOut()).setSoKeepAlive(true)
                .setTcpNoDelay(true).build();

        httpClientBuilder.setDefaultSocketConfig(socketConfig);
        connectionManager.setDefaultSocketConfig(socketConfig);

        httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(site.getRetryTimes(), true));

        generateCookie(httpClientBuilder, site);
        return httpClientBuilder.build();
    }

    private void generateCookie(CrawlerHttpClientBuilder httpClientBuilder, Site site) {
        CookieStore cookieStore = MultiUserCookieStore.getDefaultMultiUserCookieStore();
        for (Map.Entry<String, String> cookieEntry : site.getCookies().entrySet()) {
            BasicClientCookie cookie = new BasicClientCookie(cookieEntry.getKey(), cookieEntry.getValue());
            cookie.setDomain(site.getDomain());
            cookieStore.addCookie(cookie);
        }
        for (Map.Entry<String, Map<String, String>> domainEntry : site.getAllCookies().entrySet()) {
            for (Map.Entry<String, String> cookieEntry : domainEntry.getValue().entrySet()) {
                BasicClientCookie cookie = new BasicClientCookie(cookieEntry.getKey(), cookieEntry.getValue());
                cookie.setDomain(domainEntry.getKey());
                cookieStore.addCookie(cookie);
            }
        }
        httpClientBuilder.setDefaultCookieStore(cookieStore);
    }

}
