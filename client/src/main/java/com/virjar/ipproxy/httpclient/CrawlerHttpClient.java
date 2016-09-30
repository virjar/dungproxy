package com.virjar.ipproxy.httpclient;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.auth.AuthState;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.HttpClientParamConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.config.Lookup;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionRequest;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.ManagedClientConnection;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.cookie.CookieSpecProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.execchain.ClientExecChain;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpParamsNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;
import org.apache.http.util.EntityUtils;

import com.virjar.ipproxy.util.CharsetDetector;

/**
 * 包装httpclient,应该继承它 Created by virjar on 16/9/19.
 */
public class CrawlerHttpClient extends CloseableHttpClient implements Configurable {

    private static final CrawlerHttpClient instance = CrawlerHttpClientBuilder.create().build();

    public static void main(String[] args) {
        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet("www.baidu.com");
        try {
            httpClient.execute(httpGet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final Log log = LogFactory.getLog(getClass());

    private final ClientExecChain execChain;
    private final HttpClientConnectionManager connManager;
    private final HttpRoutePlanner routePlanner;
    private final Lookup<CookieSpecProvider> cookieSpecRegistry;
    private final Lookup<AuthSchemeProvider> authSchemeRegistry;
    private final CookieStore cookieStore;
    private final CredentialsProvider credentialsProvider;
    private final RequestConfig defaultConfig;
    private final List<Closeable> closeables;

    CrawlerHttpClient(final ClientExecChain execChain, final HttpClientConnectionManager connManager,
            final HttpRoutePlanner routePlanner, final Lookup<CookieSpecProvider> cookieSpecRegistry,
            final Lookup<AuthSchemeProvider> authSchemeRegistry, final CookieStore cookieStore,
            final CredentialsProvider credentialsProvider, final RequestConfig defaultConfig,
            final List<Closeable> closeables) {
        super();
        Args.notNull(execChain, "HTTP client exec chain");
        Args.notNull(connManager, "HTTP connection manager");
        Args.notNull(routePlanner, "HTTP route planner");
        this.execChain = execChain;
        this.connManager = connManager;
        this.routePlanner = routePlanner;
        this.cookieSpecRegistry = cookieSpecRegistry;
        this.authSchemeRegistry = authSchemeRegistry;
        this.cookieStore = cookieStore;
        this.credentialsProvider = credentialsProvider;
        this.defaultConfig = defaultConfig;
        this.closeables = closeables;
    }

    private HttpRoute determineRoute(final HttpHost target, final HttpRequest request, final HttpContext context)
            throws HttpException {
        HttpHost host = target;
        if (host == null) {
            host = (HttpHost) request.getParams().getParameter(ClientPNames.DEFAULT_HOST);
        }
        return this.routePlanner.determineRoute(host, request, context);
    }

    private void setupContext(final HttpClientContext context) {
        if (context.getAttribute(HttpClientContext.TARGET_AUTH_STATE) == null) {
            context.setAttribute(HttpClientContext.TARGET_AUTH_STATE, new AuthState());
        }
        if (context.getAttribute(HttpClientContext.PROXY_AUTH_STATE) == null) {
            context.setAttribute(HttpClientContext.PROXY_AUTH_STATE, new AuthState());
        }
        if (context.getAttribute(HttpClientContext.AUTHSCHEME_REGISTRY) == null) {
            context.setAttribute(HttpClientContext.AUTHSCHEME_REGISTRY, this.authSchemeRegistry);
        }
        if (context.getAttribute(HttpClientContext.COOKIESPEC_REGISTRY) == null) {
            context.setAttribute(HttpClientContext.COOKIESPEC_REGISTRY, this.cookieSpecRegistry);
        }
        if (context.getAttribute(HttpClientContext.COOKIE_STORE) == null) {
            context.setAttribute(HttpClientContext.COOKIE_STORE, this.cookieStore);
        }
        if (context.getAttribute(HttpClientContext.CREDS_PROVIDER) == null) {
            context.setAttribute(HttpClientContext.CREDS_PROVIDER, this.credentialsProvider);
        }
        if (context.getAttribute(HttpClientContext.REQUEST_CONFIG) == null) {
            context.setAttribute(HttpClientContext.REQUEST_CONFIG, this.defaultConfig);
        }
    }

    @Override
    protected CloseableHttpResponse doExecute(final HttpHost target, final HttpRequest request,
            final HttpContext context) throws IOException, ClientProtocolException {
        Args.notNull(request, "HTTP request");
        HttpExecutionAware execAware = null;
        if (request instanceof HttpExecutionAware) {
            execAware = (HttpExecutionAware) request;
        }
        try {
            final HttpRequestWrapper wrapper = HttpRequestWrapper.wrap(request, target);
            final HttpClientContext localcontext = HttpClientContext
                    .adapt(context != null ? context : new BasicHttpContext());
            RequestConfig config = null;
            if (request instanceof Configurable) {
                config = ((Configurable) request).getConfig();
            }
            if (config == null) {
                final HttpParams params = request.getParams();
                if (params instanceof HttpParamsNames) {
                    if (!((HttpParamsNames) params).getNames().isEmpty()) {
                        config = HttpClientParamConfig.getRequestConfig(params);
                    }
                } else {
                    config = HttpClientParamConfig.getRequestConfig(params);
                }
            }
            if (config != null) {
                localcontext.setRequestConfig(config);
            }
            setupContext(localcontext);
            final HttpRoute route = determineRoute(target, wrapper, localcontext);
            return this.execChain.execute(route, wrapper, localcontext, execAware);
        } catch (final HttpException httpException) {
            throw new ClientProtocolException(httpException);
        }
    }

    @Override
    public RequestConfig getConfig() {
        return this.defaultConfig;
    }

    @Override
    public void close() {
        if (this.closeables != null) {
            for (final Closeable closeable : this.closeables) {
                try {
                    closeable.close();
                } catch (final IOException ex) {
                    this.log.error(ex.getMessage(), ex);
                }
            }
        }
    }

    @Override
    public HttpParams getParams() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {

        return new ClientConnectionManager() {

            @Override
            public void shutdown() {
                connManager.shutdown();
            }

            @Override
            public ClientConnectionRequest requestConnection(final HttpRoute route, final Object state) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void releaseConnection(final ManagedClientConnection conn, final long validDuration,
                    final TimeUnit timeUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SchemeRegistry getSchemeRegistry() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void closeIdleConnections(final long idletime, final TimeUnit tunit) {
                connManager.closeIdleConnections(idletime, tunit);
            }

            @Override
            public void closeExpiredConnections() {
                connManager.closeExpiredConnections();
            }

        };

    }

    @Deprecated
    public static String getQuatity(String url, String proxyIp, int proxyPort) {
        try {
            return get(url, proxyIp, proxyPort);
        } catch (IOException e) {
            return null;
        }
    }

    @Deprecated
    public static String getQuatity(String url) {
        return getQuatity(url, null, -1);
    }

    /**
     * 这种方式要自动探测字符集,不能使用默认,作为客户端,应对的是各种目标网站,不能使用客户端所在环境的默认字符集的
     * 
     * @param url
     * @param proxyIp
     * @param proxyPort
     * @return
     * @throws IOException
     */
    public static String get(String url, String proxyIp, int proxyPort) throws IOException {
        HttpGet get = new HttpGet(url);
        if (StringUtils.isNotEmpty(proxyIp)) {
            RequestConfig build = RequestConfig.custom().setSocketTimeout(5000).setConnectTimeout(15000)
                    .setConnectionRequestTimeout(20000).setRedirectsEnabled(true).setCircularRedirectsAllowed(true)
                    .setProxy(new HttpHost(proxyIp, proxyPort)).build();
            get.setConfig(build);
        }
        CloseableHttpResponse execute = instance.execute(get);
        byte[] bytes = EntityUtils.toByteArray(execute.getEntity());
        Header[] headers = execute.getHeaders("Content-Type");
        String charset = CharsetDetector.detectHeader(headers);
        if (charset == null) {
            charset = CharsetDetector.detectHtmlContent(bytes);
        }
        if (charset == null) {
            charset = Charset.defaultCharset().name();
        }
        return new String(bytes, charset);
    }
}
