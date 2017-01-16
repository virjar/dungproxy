package com.virjar.dungproxy.client.webmagic;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

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
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.protocol.HttpContext;

import com.virjar.dungproxy.client.httpclient.CrawlerHttpClientBuilder;
import com.virjar.dungproxy.client.util.ReflectUtil;

import org.apache.http.ssl.SSLContextBuilder;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.proxy.Proxy;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * @author code4crafter@gmail.com <br>
 * @since 0.4.0
 */
public class DungProxyHttpClientGenerator {

    private PoolingHttpClientConnectionManager connectionManager;

    public DungProxyHttpClientGenerator() {
        //和webMagic不同的是,这里忽略https证书,大多数场景需要忽略证书校验
        SSLContext sslContext = null;
        try {
            sslContext = new SSLContextBuilder().loadTrustMaterial(new TrustStrategy() {
                // 信任所有
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            }).build();
        } catch (Exception e) {
            e.printStackTrace();
        }
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext);
        Registry<ConnectionSocketFactory> reg = RegistryBuilder.<ConnectionSocketFactory> create()
                .register("http", PlainConnectionSocketFactory.INSTANCE).register("https", sslsf).build();

        connectionManager = new PoolingHttpClientConnectionManager(reg);
        connectionManager.setDefaultMaxPerRoute(100);
    }

    public DungProxyHttpClientGenerator setPoolSize(int poolSize) {
        connectionManager.setMaxTotal(poolSize);
        return this;
    }

    public CloseableHttpClient getClient(Site site, Proxy proxy) {
        return generateClient(site, proxy);
    }

    /**
     * 在webMagic 0.5.3上面强行调用webMagic 0.6.x的API ,
     * webMagic的0.6.1估计会严重改动代理相关逻辑,和dungProxy的冲突应该不小,暂时尝试做一段时间兼容。不行的话主版本调整位0.6.x,兼容低版本的0.5.3
     * 
     * @param site site
     * @param proxy 代理
     * @param httpClientBuilder httpclient构造器
     */
    private void handHighApi(Site site, Proxy proxy, CrawlerHttpClientBuilder httpClientBuilder) {
        CredentialsProvider credsProvider;
        if (proxy != null) {
            String user = ReflectUtil.invoke(proxy, "getUser", new Class[] {}, new Object[] {});
            String password = ReflectUtil.invoke(proxy, "getPassword", new Class[] {}, new Object[] {});
            if (StringUtils.isNotEmpty(user) && StringUtils.isNotEmpty(password)) {
                credsProvider = new BasicCredentialsProvider();
                credsProvider.setCredentials(
                        new AuthScope(proxy.getHttpHost().getAddress().getHostAddress(), proxy.getHttpHost().getPort()),
                        new UsernamePasswordCredentials(user, password));
                httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
            }
        }

        if (site == null || site.getHttpProxy() == null) {
            return;
        }
        Credentials usernamePasswordCredentials = ReflectUtil.invoke(site, "getUsernamePasswordCredentials",
                new Class[] {}, new Object[] {});
        if (usernamePasswordCredentials != null) {
            credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(site.getHttpProxy()), // 可以访问的范围
                    usernamePasswordCredentials);// 用户名和密码
            httpClientBuilder.setDefaultCredentialsProvider(credsProvider);
        }
    }

    private CloseableHttpClient generateClient(Site site, Proxy proxy) {

        // 这里替换成我们的,才能方便的使用dungProxy的IP池
        CrawlerHttpClientBuilder httpClientBuilder = CrawlerHttpClientBuilder.create();

        if (proxy != null && ReflectUtil.hasMethod(proxy, "getUsernamePasswordCredentials")) {// 是webMagic 6.x的新API
            handHighApi(site, proxy, httpClientBuilder);
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

        SocketConfig socketConfig = SocketConfig.custom().setSoTimeout(site.getTimeOut()).setSoKeepAlive(true)
                .setTcpNoDelay(true).build();


        httpClientBuilder.setDefaultSocketConfig(socketConfig);
        connectionManager.setDefaultSocketConfig(socketConfig);

        //ignoreSSLCertificate(httpClientBuilder);

        httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(site.getRetryTimes(), true));

        generateCookie(httpClientBuilder, site);
        return httpClientBuilder.build();
    }

    /**
     * 忽略https证书
     * @param httpClientBuilder httpclient构建器
     */
    private void ignoreSSLCertificate(CrawlerHttpClientBuilder httpClientBuilder){
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
        CookieStore cookieStore = new BasicCookieStore();
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
