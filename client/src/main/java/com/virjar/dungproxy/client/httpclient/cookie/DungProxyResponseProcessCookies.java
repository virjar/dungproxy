package com.virjar.dungproxy.client.httpclient.cookie;

import java.io.IOException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.*;
import org.apache.http.annotation.Immutable;
import org.apache.http.client.CookieStore;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.cookie.*;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.Args;

import com.virjar.dungproxy.client.ippool.config.ProxyConstant;
import com.virjar.dungproxy.client.util.CommonUtil;

/**
 * Response interceptor that populates the current {@link CookieStore} with data contained in response cookies received
 * in the given the HTTP response. <br/>
 * 修改自cookie拦截器,如果是在原生httpclient里面集成,则需要禁止cookie功能,然后手动添加这个拦截器。CrawlerHttpClient则是默认使用这个cookie拦截器了
 * 
 * @since 0.0.4
 * @see org.apache.http.client.protocol.ResponseProcessCookies
 */
@Immutable
public class DungProxyResponseProcessCookies implements HttpResponseInterceptor {

    private final Log log = LogFactory.getLog(getClass());

    public DungProxyResponseProcessCookies() {
        super();
    }

    @Override
    public void process(final HttpResponse response, final HttpContext context) throws HttpException, IOException {
        Args.notNull(response, "HTTP request");
        Args.notNull(context, "HTTP context");

        final HttpClientContext clientContext = HttpClientContext.adapt(context);

        // Obtain actual CookieSpec instance
        final CookieSpec cookieSpec = clientContext.getCookieSpec();
        if (cookieSpec == null) {
            this.log.debug("Cookie spec not specified in HTTP context");
            return;
        }
        // Obtain cookie store
        final CookieStore cookieStore = clientContext.getCookieStore();
        if (cookieStore == null) {
            this.log.debug("Cookie store not specified in HTTP context");
            return;
        }
        // Obtain actual CookieOrigin instance
        final CookieOrigin cookieOrigin = clientContext.getCookieOrigin();
        if (cookieOrigin == null) {
            this.log.debug("Cookie origin not specified in HTTP context");
            return;
        }
        HeaderIterator it = response.headerIterator(SM.SET_COOKIE);
        processCookies(it, cookieSpec, cookieOrigin, cookieStore, clientContext);

        // see if the cookie spec supports cookie versioning.
        if (cookieSpec.getVersion() > 0) {
            // process set-cookie2 headers.
            // Cookie2 will replace equivalent Cookie instances
            it = response.headerIterator(SM.SET_COOKIE2);
            processCookies(it, cookieSpec, cookieOrigin, cookieStore, clientContext);
        }
    }

    private void addCookie(CookieStore cookieStore, Cookie cookie, HttpClientContext clientContext) {
        if (cookieStore instanceof MultiUserCookieStore) {
            MultiUserCookieStore multiUserCookieStore = (MultiUserCookieStore) cookieStore;
            multiUserCookieStore.addCookie(cookie,
                    CommonUtil.safeToString(clientContext.getAttribute(ProxyConstant.DUNGPROXY_USER_KEY)));
        } else {
            cookieStore.addCookie(cookie);
        }
    }

    private void processCookies(final HeaderIterator iterator, final CookieSpec cookieSpec,
            final CookieOrigin cookieOrigin, final CookieStore cookieStore, HttpClientContext clientContext) {

        while (iterator.hasNext()) {
            final Header header = iterator.nextHeader();
            try {
                final List<Cookie> cookies = cookieSpec.parse(header, cookieOrigin);
                for (final Cookie cookie : cookies) {
                    try {
                        cookieSpec.validate(cookie, cookieOrigin);
                        addCookie(cookieStore, cookie, clientContext);

                        if (this.log.isDebugEnabled()) {
                            this.log.debug("Cookie accepted [" + formatCooke(cookie) + "]");
                        }
                    } catch (final MalformedCookieException ex) {
                        if (this.log.isWarnEnabled()) {
                            this.log.warn("Cookie rejected [" + formatCooke(cookie) + "] " + ex.getMessage());
                        }
                    }
                }
            } catch (final MalformedCookieException ex) {
                if (this.log.isWarnEnabled()) {
                    this.log.warn("Invalid cookie header: \"" + header + "\". " + ex.getMessage());
                }
            }
        }
    }

    private static String formatCooke(final Cookie cookie) {
        final StringBuilder buf = new StringBuilder();
        buf.append(cookie.getName());
        buf.append("=\"");
        String v = cookie.getValue();
        if (v != null) {
            if (v.length() > 100) {
                v = v.substring(0, 100) + "...";
            }
            buf.append(v);
        }
        buf.append("\"");
        buf.append(", version:");
        buf.append(Integer.toString(cookie.getVersion()));
        buf.append(", domain:");
        buf.append(cookie.getDomain());
        buf.append(", path:");
        buf.append(cookie.getPath());
        buf.append(", expiry:");
        buf.append(cookie.getExpiryDate());
        return buf.toString();
    }

}
