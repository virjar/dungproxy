package com.virjar.dungproxy.webmagic6;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.HttpContext;
import org.apache.http.ssl.SSLContextBuilder;

import com.virjar.dungproxy.client.httpclient.CrawlerHttpClient;
import com.virjar.dungproxy.client.httpclient.CrawlerHttpClientBuilder;
import com.virjar.dungproxy.client.httpclient.cookie.MultiUserCookieStore;
import com.virjar.dungproxy.client.util.ReflectUtil;

import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.downloader.CustomRedirectStrategy;
import us.codecraft.webmagic.proxy.Proxy;

/**
 * @author code4crafter@gmail.com <br>
 * @since 0.4.0
 */
public class DungProxyHttpClientGenerator {

    private PoolingHttpClientConnectionManager connectionManager;

    public DungProxyHttpClientGenerator() {

        connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setDefaultMaxPerRoute(100);
    }

    public DungProxyHttpClientGenerator setPoolSize(int poolSize) {
        connectionManager.setMaxTotal(poolSize);
        return this;
    }

    public CrawlerHttpClient getClient(Site site, Proxy proxy) {
        return generateClient(site, proxy);
    }



    private CrawlerHttpClient generateClient(Site site, Proxy proxy) {
        CredentialsProvider credsProvider = null;
        // 这里替换成我们的,才能方便的使用dungProxy的IP池
        CrawlerHttpClientBuilder httpClientBuilder = CrawlerHttpClientBuilder.create();
        if(proxy!=null && StringUtils.isNotBlank(proxy.getUser()) && StringUtils.isNotBlank(proxy.getPassword()))
        {
            credsProvider= new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(proxy.getHttpHost().getAddress().getHostAddress(), proxy.getHttpHost().getPort()),
                    new UsernamePasswordCredentials(proxy.getUser(), proxy.getPassword()));
            httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
        }

        if(site!=null&&site.getHttpProxy()!=null&&site.getUsernamePasswordCredentials()!=null){
            credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(site.getHttpProxy()),//可以访问的范围
                    site.getUsernamePasswordCredentials());//用户名和密码
            httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
        }

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

        //解决post/redirect/post 302跳转问题
        httpClientBuilder.setRedirectStrategy(new CustomRedirectStrategy());

        SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(site.getTimeOut()).setSoKeepAlive(true)
                .setTcpNoDelay(true).build();

        httpClientBuilder.setDefaultSocketConfig(socketConfig);
        connectionManager.setDefaultSocketConfig(socketConfig);


        httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(site.getRetryTimes(), true));

        generateCookie(httpClientBuilder, site);
        return httpClientBuilder.build();
    }

    /**
     * 忽略https证书
     * 
     * @param httpClientBuilder httpclient构建器
     */
    private void ignoreSSLCertificate(CrawlerHttpClientBuilder httpClientBuilder) {
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
        httpClientBuilder.setSSLContext(sslContext);
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
