package com.virjar.utils.net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.AbstractHttpMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.virjar.utils.NullUtil;

public class HttpInvoker {

    private String url;
    private boolean proxyenable = false;
    private boolean relocationnable = false;
    private String proxyIp;
    private int proxyPort;
    private static HttpClient httpClient = HttpClientBuilder.create().build();
    private static final Logger logger = LoggerFactory.getLogger(HttpInvoker.class);
    private static final WebClient webClient = new WebClient();

    public HttpInvoker setproxy(String ip, int port) {
        proxyIp = ip;
        proxyPort = port;
        proxyenable = true;
        return this;
    }

    public boolean isProxyenable() {
        return proxyenable;
    }

    public void setProxyenable(boolean proxyenable) {
        this.proxyenable = proxyenable;
    }

    public boolean isRelocationnable() {
        return relocationnable;
    }

    public void setRelocationnable(boolean relocationnable) {
        this.relocationnable = relocationnable;
    }

    private String lastUrl;

    public HttpInvoker(String url) {
        this.url = url;
    }

    public HttpResult requestFromHtmlUnit(HtmlUnitResultWait waitHandler) {
        webClient.getOptions().setThrowExceptionOnScriptError(false);
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        webClient.getOptions().setJavaScriptEnabled(true);
        webClient.getOptions().setActiveXNative(false);
        webClient.getOptions().setCssEnabled(false);
        webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        try {
            HtmlPage page = webClient.getPage(url);
            if (waitHandler != null) {
                long timeOut = waitHandler.getTimeOut();
                long interverl = timeOut / 100;
                if (interverl < 20)
                    interverl = 20;

                long times = timeOut / interverl;
                if (times < 1)
                    times = 1;

                long testTime = 0;

                while (testTime < times) {
                    if (waitHandler.canReturn(page.asXml(), page.getUrl().toString())) {
                        return new HttpResult(200, null, page.asXml());
                    }
                    testTime++;
                    Thread.sleep(interverl);
                }
            }
            lastUrl = page.getUrl().toString();
            return new HttpResult(200, null, page.asXml());
        } catch (Exception e) {
            logger.error("htmlUnit request failed", e);
        } finally {
            // webClient.closeAllWindows();
        }

        return new HttpResult(-1, null, null);
    }

    public HttpResult postFromHtmlUnit(HtmlUnitResultWait waitHandler, List<NameValuePair> nvps, String orign,
            String referer, List<NameValuePair> headers) {
        try {
            WebClient webClient = new WebClient();

            webClient.getOptions().setThrowExceptionOnScriptError(false);
            webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
            webClient.getOptions().setJavaScriptEnabled(true);
            webClient.getOptions().setActiveXNative(false);
            webClient.getOptions().setCssEnabled(false);
            webClient.setAjaxController(new NicelyResynchronizingAjaxController());

            WebRequest webRequest = new WebRequest(new URL(url), com.gargoylesoftware.htmlunit.HttpMethod.POST);
            if (headers != null) {
                for (NameValuePair head : headers) {
                    webRequest.setAdditionalHeader(head.getName(), head.getValue());
                }
            }
            webClient.loadWebResponse(webRequest);
        } catch (Exception e) {
            logger.error("htmlUnit request failed", e);
        }
        return new HttpResult(-1, null, null);
    }

    private void setHeader(AbstractHttpMessage method, String origin, String referer) {
        method.addHeader("Connection", "keep-alive");
        method.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        method.addHeader("User-Agent",
                "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/38.0.2125.122 Safari/537.36");
        method.addHeader("Accept-Encoding", "gzip,deflate");
        method.addHeader("Accept-Language", "zh-CN,zh;q=0.8");
        if (!NullUtil.isNull(origin))
            method.addHeader("Origin", origin);
        if (!NullUtil.isNull(referer))
            method.addHeader("Referer", referer);
    }

    public HttpResult post(List<NameValuePair> nvps, String orign, String referer,
            List<? extends NameValuePair> headers) {
        InputStream content = null;
        try {
            HttpPost post = new HttpPost(url);

            setHeader(post, orign, referer);

            if (headers != null) {
                for (NameValuePair nameValuepair : headers) {
                    // post.addRequestHeader(nameValuepair.getName(), nameValuepair.getValue());
                    post.addHeader(nameValuepair.getName(), nameValuepair.getValue());
                }

            }

            post.setEntity(new UrlEncodedFormEntity(nvps));

            HttpResponse response = httpClient.execute(post);
            content = response.getEntity().getContent();
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                return new HttpResult(statusCode, response.getEntity().toString());
            } else if (statusCode == 302) {
                Header header = response.getHeaders("Location")[0];
                String location = null;
                if (header != null) {
                    location = header.getValue();
                }
                return new HttpResult(statusCode, location, IOUtils.toString(content));
            } else {
                return new HttpResult(statusCode);
            }

        } catch (Exception e) {
            logger.error("net work error", e);
        } finally {
            IOUtils.closeQuietly(content);
        }
        return new HttpResult(-1, null, null);
    }

    public HttpResult request() throws IOException {
        HttpGet getMethod = new HttpGet(url);
        RequestConfig.Builder builder = RequestConfig.custom();
        if (!relocationnable) {
            builder.setRedirectsEnabled(false);
        }
        InputStream content = null;
        ByteArrayOutputStream out = null;
        GZIPInputStream gunzip = null;
        try {
            if (proxyenable) {
                builder.setProxy(new HttpHost(proxyIp, proxyPort));
            }

            builder.setConnectTimeout(0);
            builder.setSocketTimeout(0);
            builder.setConnectionRequestTimeout(0);
            // 设置UA
            setHeader(getMethod, null, null);
            getMethod.setConfig(builder.build());
            HttpResponse response = httpClient.execute(getMethod);
            int statusCode = response.getStatusLine().getStatusCode();
            content = response.getEntity().getContent();
            lastUrl = getMethod.getURI().toString();

            if (statusCode == 200) {
                Header[] headers = response.getHeaders("Content-Encoding");
                if (headers != null && headers.length > 0 && headers[0].getValue().toLowerCase().contains("gzip")) {
                    out = new ByteArrayOutputStream();
                    gunzip = new GZIPInputStream(response.getEntity().getContent());
                    byte[] buffer = new byte[256];
                    int n;
                    while ((n = gunzip.read(buffer)) >= 0) {
                        out.write(buffer, 0, n);
                    }
                    return new HttpResult(response.getStatusLine().getStatusCode(), new String(out.toByteArray()));
                }
                return new HttpResult(statusCode, IOUtils.toString(content));
            } else if (response.getStatusLine().getStatusCode() == 302) {
                Header header =  response.getHeaders("Location")[0];
                String location = null;
                if (header != null) {
                    location = header.getValue();
                }
                return new HttpResult(statusCode, location, IOUtils.toString(content));
            } else {
                return new HttpResult(statusCode);
            }
        } finally {
            IOUtils.closeQuietly(content);
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(gunzip);
            getMethod.releaseConnection();
        }

    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLastUrl() {
        return lastUrl;
    }

    public void setLastUrl(String lastUrl) {
        this.lastUrl = lastUrl;
    }
}
