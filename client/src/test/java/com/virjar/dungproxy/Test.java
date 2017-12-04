package com.virjar.dungproxy;

import com.virjar.dungproxy.client.ippool.GroupBindRouter;
import com.virjar.dungproxy.client.ippool.config.DungProxyContext;
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
        GroupBindRouter groupBindRouter = new GroupBindRouter();
        groupBindRouter.buildRule("91.91p18.space:*");
        DungProxyContext.create().setGroupBindRouter(groupBindRouter).setPoolEnabled(true);

        // 先创建一个httpclient,预热一下,方便调试
        CrawlerHttpClient crawlerHttpClient = buildDefault();
        crawlerHttpClient.get("http://www.java1234.com/index.html");

        // 重新穿件一个httpclient,避免链接池服用
        crawlerHttpClient = buildDefault();
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
