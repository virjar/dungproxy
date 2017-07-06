package com.virjar.dungproxy;

import org.apache.http.client.CookieStore;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.LaxRedirectStrategy;

import com.virjar.dungproxy.client.httpclient.CrawlerHttpClient;
import com.virjar.dungproxy.client.httpclient.CrawlerHttpClientBuilder;
import com.virjar.dungproxy.client.httpclient.conn.logging.PlainTextLoggingConnectionFactory;
import com.virjar.dungproxy.client.httpclient.cookie.BarrierCookieStore;
import com.virjar.dungproxy.client.httpclient.cookie.CookieStoreGenerator;
import com.virjar.dungproxy.client.httpclient.cookie.MultiUserCookieStore;
import com.virjar.dungproxy.client.ippool.config.ProxyConstant;

/**
 * Created by virjar on 17/6/27.
 */
public class Test {
    public static void main(String[] args) {

        CrawlerHttpClient crawlerHttpClient = buildDefault();
        crawlerHttpClient.get("http://www.java1234.com/index.html");
        crawlerHttpClient.get("http://www.java1234.com/index.html");
    }

    public static CrawlerHttpClient buildDefault() {
        SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(true).setSoLinger(-1).setSoReuseAddress(false)
                .setSoTimeout(ProxyConstant.SOCKETSO_TIMEOUT).setTcpNoDelay(true).build();

        return CrawlerHttpClientBuilder.create().setMaxConnTotal(1000).setMaxConnPerRoute(50)
                .setDefaultSocketConfig(socketConfig).setConnFactory(new PlainTextLoggingConnectionFactory())
                // .setSSLSocketFactory(sslConnectionSocketFactory) 证书忽略逻辑转移到httpclient builder里面
                .setRedirectStrategy(new LaxRedirectStrategy())
                .setDefaultCookieStore(new MultiUserCookieStore(new CookieStoreGenerator() {
                    @Override
                    public CookieStore generate(String user) {
                        return new BarrierCookieStore();
                    }
                })).build();
    }

}
