package com.virjar.dungproxy.client.samples.geetest;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.LaxRedirectStrategy;

import com.virjar.dungproxy.client.httpclient.CrawlerHttpClient;
import com.virjar.dungproxy.client.httpclient.CrawlerHttpClientBuilder;
import com.virjar.dungproxy.client.httpclient.cookie.CookieDisableCookieStore;
import com.virjar.dungproxy.client.ippool.PreHeater;
import com.virjar.dungproxy.client.ippool.config.DungProxyContext;
import com.virjar.dungproxy.client.ippool.config.ProxyConstant;
import com.virjar.dungproxy.client.ippool.strategy.ProxyChecker;
import com.virjar.dungproxy.client.ippool.strategy.impl.JSONFileAvProxyDumper;
import com.virjar.dungproxy.client.ippool.strategy.impl.WhiteListProxyStrategy;
import com.virjar.dungproxy.client.model.AvProxyVO;

/**
 * Created by virjar on 17/3/18. <br/>
 * 用来产生滑块验证码所需要的代理
 */
public class GeetestPreHeater {
    public static void main(String[] args) {
        // 指向空是为了避免和其他sample引起冲突,因为他比默认加载一个配置文件的规则
        ProxyConstant.CLIENT_CONFIG_FILE_NAME = "";

        DungProxyContext dungProxyContext = DungProxyContext.create()
                .setAvProxyDumper(new JSONFileAvProxyDumper("/Users/virjar/Desktop/geetest_proxy.json"))
                .setNeedProxyStrategy(new WhiteListProxyStrategy("api.geetest.com"));
        dungProxyContext.genDomainContext("api.geetest.com").setProxyChecker(new GeeProxyChecker());// 定制IP检查器

        PreHeater preHeater = dungProxyContext.getPreHeater();
        preHeater.addTask(
                "http://api.geetest.com/get.php?gt=a40fd3b0d712165c5d13e6f747e948d4&product=embed&offline=false&protocol=&type=slide&callback=geetest_1489852563664");
        preHeater.doPreHeat();
    }

    /**
     * 检查是否可以作为滑块验证码的代理,滑块验证码每次请求都在一个session中,所以不能中途拦截URL作为检查URL
     */
    private static class GeeProxyChecker implements ProxyChecker {

        private CrawlerHttpClient crawlerHttpClient = buildHttpClient();

        @Override
        public boolean available(AvProxyVO avProxyVO, String url) {
            for (int i = 0; i < 3; i++) {
                String s = crawlerHttpClient
                        .get("http://api.geetest.com/get.php?gt=a40fd3b0d712165c5d13e6f747e948d4&product=embed&offline=false&protocol=&type=slide&callback=geetest_"
                                + System.currentTimeMillis(), avProxyVO.getIp(), avProxyVO.getPort());
                // 从响应里面的几个关键字来判断是否被封禁,这几个关键字是极验逻辑必须的
                if (StringUtils.contains(s, "challenge") && StringUtils.contains(s, "gt")
                        && StringUtils.contains(s, "ypos")) {
                    return true;
                }
            }
            return false;
        }

        /**
         * 构造一个httpclient,这个httpclient没有cookie数据,所以不会和其他用户冲突,也可以正常的进行检查,cookie 禁用只能是检查的时候,实际和极验交互不能这么做
         * 
         * @return 一个不具有cookie植入功能的httpclient,
         */
        private static CrawlerHttpClient buildHttpClient() {
            SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(true).setSoLinger(-1)
                    .setSoReuseAddress(false).setSoTimeout(ProxyConstant.SOCKETSO_TIMEOUT).setTcpNoDelay(true).build();
            X509TrustManager x509mgr = new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] xcs, String string) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] xcs, String string) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            };

            SSLContext sslContext = null;
            try {
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[] { x509mgr }, null);
            } catch (Exception e) {
                //// TODO: 16/11/23
            }

            SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
                    SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            return CrawlerHttpClientBuilder.create().setMaxConnTotal(1000).setMaxConnPerRoute(50)
                    .setDefaultSocketConfig(socketConfig).setSSLSocketFactory(sslConnectionSocketFactory)
                    .setRedirectStrategy(new LaxRedirectStrategy())
                    // 禁用cookie有多种方式,这是其中一种而已
                    .setDefaultCookieStore(new CookieDisableCookieStore()).build();
        }

    }
}
