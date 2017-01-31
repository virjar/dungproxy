package com.virjar.dungproxy.client.httpclient.conn;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.virjar.dungproxy.client.ippool.IpPool;
import com.virjar.dungproxy.client.ippool.config.ProxyConstant;
import com.virjar.dungproxy.client.model.AvProxy;

/**
 * 如果是和原生httpclient集成,必须将她放置到httpclientBuilder中,如果使用我们默认提供的,则无需设置,他将会自动植入<br/>
 * 通过这个绑定代理IP,这种方式可以以非侵入的形式和原声httpclient集成,<br/>
 * 甚至只要在原声httpclientBuilder里面植入本类,即可实现代理自动接入服务<br/>
 * Created by virjar on 16/9/28.
 */
public class ProxyBindRoutPlanner extends DefaultRoutePlanner {

    public ProxyBindRoutPlanner() {
        super(null);
    }

    private static final Logger logger = LoggerFactory.getLogger(ProxyBindRoutPlanner.class);

    /**
     * @param schemePortResolver schema解析器,可以传空,这个时候将会使用默认
     *            {org.apache.http.impl.conn.DefaultSchemePortResolver#INSTANCE}
     */
    public ProxyBindRoutPlanner(SchemePortResolver schemePortResolver) {
        super(schemePortResolver);
    }

    @Override
    protected HttpHost determineProxy(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {

        String accessUrl = null;
        if (request instanceof HttpRequestWrapper || request instanceof HttpGet) {
            accessUrl = HttpUriRequest.class.cast(request).getURI().toString();
        }

        AvProxy bind = (AvProxy) context.getAttribute(ProxyConstant.USED_PROXY_KEY);
        if (bind == null || bind.isDisable()) {
            bind = IpPool.getInstance().bind(target.getHostName(), accessUrl);
        }

        if (bind == null) {
            return super.determineProxy(target, request, context);
        }

        logger.info("{} 当前使用IP为:{}:{}", target.getHostName(), bind.getIp(), bind.getPort());
        bind.recordUsage();
        // 将绑定IP放置到context,用于后置拦截器统计这个IP的使用情况
        context.setAttribute(ProxyConstant.USED_PROXY_KEY, bind);

        // 如果代理有认证头部,则注入认证头部
        if (bind.getAuthenticationHeaders() != null) {
            for (Header header : bind.getAuthenticationHeaders()) {
                request.addHeader(header);
            }
        }

        // 注入用户名密码
        if (StringUtils.isNotEmpty(bind.getUsername()) && StringUtils.isNotEmpty(bind.getPassword())) {
            HttpClientContext httpClientContext = HttpClientContext.adapt(context);
            CredentialsProvider credsProvider = httpClientContext.getCredentialsProvider();
            if (credsProvider == null) {
                credsProvider = new BasicCredentialsProvider();
                httpClientContext.setCredentialsProvider(credsProvider);
            }
            credsProvider.setCredentials(new AuthScope(target),
                    new UsernamePasswordCredentials(bind.getUsername(), bind.getPassword()));
        }
        return new HttpHost(bind.getIp(), bind.getPort());
    }
}
