package com.virjar.dungproxy.client.webmagic;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.virjar.dungproxy.client.util.PoolUtil;
import com.virjar.dungproxy.client.util.ReflectUtil;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.AbstractDownloader;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.selector.PlainText;
import us.codecraft.webmagic.utils.HttpConstant;
import us.codecraft.webmagic.utils.UrlUtils;

/**
 * The http downloader based on HttpClient.
 *
 * * 为webMagic实现的downloader,如果有其他定制需求,参考本类实现即可<br/>
 *
 * <pre>
 * public static void main(String[] args) {
 *     Spider.create(new GithubRepoPageProcessor()).addUrl("https://github.com/code4craft")
 *             .setDownloader(new DungProxyDownloader()).thread(5).run();
 * }
 * </pre>
 *
 * <pre>
 *     如果自己实现代理池到httpclient的织入:
 *    CloseableHttpClient closeableHttpClient =
 *          HttpClientBuilder.create().setRetryHandler(new DunProxyHttpRequestRetryHandler())
 *          .setRoutePlanner(new ProxyBindRoutPlanner()).build();
 * </pre>
 *
 * @author code4crafter@gmail.com <br>
 * @author virjar
 * @since 0.0.0
 */
@ThreadSafe
public class DungProxyDownloader extends AbstractDownloader {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, CloseableHttpClient> httpClients = new HashMap<String, CloseableHttpClient>();

    // 自动代理替换了这里
    private DungProxyHttpClientGenerator httpClientGenerator = new DungProxyHttpClientGenerator();

    private CloseableHttpClient getHttpClient(Site site, Proxy proxy) {
        if (site == null) {
            return httpClientGenerator.getClient(null, proxy);
        }
        String domain = site.getDomain();
        CloseableHttpClient httpClient = httpClients.get(domain);
        if (httpClient == null) {
            synchronized (this) {
                httpClient = httpClients.get(domain);
                if (httpClient == null) {
                    httpClient = httpClientGenerator.getClient(site, proxy);
                    httpClients.put(domain, httpClient);
                }
            }
        }
        return httpClient;
    }

    @Override
    public Page download(Request request, Task task) {
        Site site = null;
        if (task != null) {
            site = task.getSite();
        }
        Set<Integer> acceptStatCode;
        String charset = null;
        Map<String, String> headers = null;
        if (site != null) {
            acceptStatCode = site.getAcceptStatCode();
            charset = site.getCharset();
            headers = site.getHeaders();
        } else {
            acceptStatCode = Sets.newHashSet(200);// 使用guava等价替换 WMCollections.newHashSet(200);
        }
        logger.info("downloading page {}", request.getUrl());
        CloseableHttpResponse httpResponse = null;
        int statusCode = 0;
        HttpClientContext httpClientContext = null;
        try {
            HttpHost proxyHost = null;
            Proxy proxy = null; // TODO
            if (site != null && site.getHttpProxyPool() != null && site.getHttpProxyPool().isEnable()) {
                Object proxyObject = ReflectUtil.invoke(site, "getHttpProxyFromPool", new Class[] {}, new Object[] {});// site.getHttpProxyFromPool();
                // 在0.6.x下面,返回类型是Proxy,所以虽然编译器报警,但是也只能忽略语法检查,因为不同版本的webMagic会走不同的分之
                if (proxyObject instanceof HttpHost) {// 0.5.x的用法
                    proxyHost = (HttpHost) proxyObject;
                } else if (proxyObject instanceof Proxy) {// 0.6.x的用法
                    proxy = (Proxy) proxyObject;
                    proxyHost = proxy.getHttpHost();
                }

            } else if (site != null && site.getHttpProxy() != null) {
                proxyHost = site.getHttpProxy();
            }

            HttpUriRequest httpUriRequest = getHttpUriRequest(request, site, headers, proxyHost);
            httpClientContext = HttpClientContext.adapt(new BasicHttpContext());
            httpResponse = getHttpClient(site, proxy).execute(httpUriRequest, httpClientContext);
            statusCode = httpResponse.getStatusLine().getStatusCode();
            request.putExtra(Request.STATUS_CODE, statusCode);
            if (statusAccept(acceptStatCode, statusCode)) {
                Page page = handleResponse(request, charset, httpResponse, task);
                if (needOfflineProxy(page)) {
                    PoolUtil.offline(httpClientContext);
                    return addToCycleRetry(request, site);
                }
                onSuccess(request);
                return page;
            } else {
                PoolUtil.offline(httpClientContext);// webMagic对状态码的拦截可能出现在这里,所以也要在这里下线IP
                logger.warn("get page {} error, status code {} ", request.getUrl(), statusCode);
                return addToCycleRetry(request, site);
            }
        } catch (IOException e) {
            if (needOfflineProxy(e)) {
                logger.warn("发生异常:{},IP下线");
                PoolUtil.offline(httpClientContext);//由IP异常导致,直接重试
                return addToCycleRetry(request, site);
            }
            if (isLastRetry(request, site)) {// 移动异常日志位置,只记录最终失败的。中途失败不算失败
                logger.warn("download page {} error", request.getUrl(), e);
            }
            if (site != null && site.getCycleRetryTimes() > 0) {
                return addToCycleRetry(request, site);
            }
            onError(request);
            return null;
        } finally {
            request.putExtra(Request.STATUS_CODE, statusCode);
            if (site != null && site.getHttpProxyPool() != null && site.getHttpProxyPool().isEnable()) {
                site.returnHttpProxyToPool((HttpHost) request.getExtra(Request.PROXY),
                        (Integer) request.getExtra(Request.STATUS_CODE));
            }
            try {
                if (httpResponse != null) {
                    // ensure the connection is released back to pool
                    EntityUtils.consume(httpResponse.getEntity());
                }
            } catch (IOException e) {
                logger.warn("close response fail", e);
            }
        }
    }

    /**
     * 判断当前请求是不是最后的重试,流程等同于 addToCycleRetry
     * 
     * @see us.codecraft.webmagic.downloader.AbstractDownloader#addToCycleRetry(us.codecraft.webmagic.Request,
     *      us.codecraft.webmagic.Site)
     * @param request request
     * @param site site
     * @return 是否是最后一次重试
     */
    protected boolean isLastRetry(Request request, Site site) {
        Object cycleTriedTimesObject = request.getExtra(Request.CYCLE_TRIED_TIMES);
        if (cycleTriedTimesObject == null) {
            return false;
        } else {
            int cycleTriedTimes = (Integer) cycleTriedTimesObject;
            cycleTriedTimes++;
            if (cycleTriedTimes >= site.getCycleRetryTimes()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 默认封禁403和401两个状态码的IP
     * 
     * @param page 爬取结果
     * @return 是否需要封禁这个IP
     */
    protected boolean needOfflineProxy(Page page) {
        Integer statusCode = (Integer) page.getRequest().getExtra(Request.STATUS_CODE);
        if (statusCode == null) {
            return false;// 不知道状态码
        }
        return statusCode == 401 || statusCode == 403;// 401和403两个状态强制下线IP
    }

    protected boolean needOfflineProxy(IOException e) {
        return false;
    }

    @Override
    public void setThread(int thread) {
        httpClientGenerator.setPoolSize(thread);
    }

    protected boolean statusAccept(Set<Integer> acceptStatCode, int statusCode) {
        return acceptStatCode.contains(statusCode);
    }

    protected HttpUriRequest getHttpUriRequest(Request request, Site site, Map<String, String> headers,
            HttpHost proxy) {
        RequestBuilder requestBuilder = selectRequestMethod(request).setUri(request.getUrl());
        if (headers != null) {
            for (Map.Entry<String, String> headerEntry : headers.entrySet()) {
                requestBuilder.addHeader(headerEntry.getKey(), headerEntry.getValue());
            }
        }
        RequestConfig.Builder requestConfigBuilder = RequestConfig.custom()
                .setConnectionRequestTimeout(site.getTimeOut()).setSocketTimeout(site.getTimeOut())
                .setConnectTimeout(site.getTimeOut()).setCookieSpec(CookieSpecs.BEST_MATCH);
        if (proxy != null) {
            requestConfigBuilder.setProxy(proxy);
            request.putExtra(Request.PROXY, proxy);
        }
        requestBuilder.setConfig(requestConfigBuilder.build());
        return requestBuilder.build();
    }

    protected RequestBuilder selectRequestMethod(Request request) {
        String method = request.getMethod();
        if (method == null || method.equalsIgnoreCase(HttpConstant.Method.GET)) {
            // default get
            return RequestBuilder.get();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.POST)) {
            RequestBuilder requestBuilder = RequestBuilder.post();
            NameValuePair[] nameValuePair = (NameValuePair[]) request.getExtra("nameValuePair");
            if (nameValuePair != null && nameValuePair.length > 0) {
                requestBuilder.addParameters(nameValuePair);
            }
            return requestBuilder;
        } else if (method.equalsIgnoreCase(HttpConstant.Method.HEAD)) {
            return RequestBuilder.head();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.PUT)) {
            return RequestBuilder.put();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.DELETE)) {
            return RequestBuilder.delete();
        } else if (method.equalsIgnoreCase(HttpConstant.Method.TRACE)) {
            return RequestBuilder.trace();
        }
        throw new IllegalArgumentException("Illegal HTTP Method " + method);
    }

    protected Page handleResponse(Request request, String charset, HttpResponse httpResponse, Task task)
            throws IOException {
        String content = getContent(charset, httpResponse);
        Page page = new Page();
        page.setRawText(content);
        page.setUrl(new PlainText(request.getUrl()));
        page.setRequest(request);
        page.setStatusCode(httpResponse.getStatusLine().getStatusCode());
        return page;
    }

    protected String getContent(String charset, HttpResponse httpResponse) throws IOException {
        if (charset == null) {
            byte[] contentBytes = IOUtils.toByteArray(httpResponse.getEntity().getContent());
            String htmlCharset = getHtmlCharset(httpResponse, contentBytes);
            if (htmlCharset != null) {
                return new String(contentBytes, htmlCharset);
            } else {
                logger.warn("Charset autodetect failed, use {} as charset. Please specify charset in Site.setCharset()",
                        Charset.defaultCharset());
                return new String(contentBytes);
            }
        } else {
            return IOUtils.toString(httpResponse.getEntity().getContent(), charset);
        }
    }

    protected String getHtmlCharset(HttpResponse httpResponse, byte[] contentBytes) throws IOException {
        String charset;
        // charset
        // 1、encoding in http header Content-Type
        String value = httpResponse.getEntity().getContentType().getValue();
        charset = UrlUtils.getCharset(value);
        if (StringUtils.isNotBlank(charset)) {
            logger.debug("Auto get charset: {}", charset);
            return charset;
        }
        // use default charset to decode first time
        Charset defaultCharset = Charset.defaultCharset();
        String content = new String(contentBytes, defaultCharset.name());
        // 2、charset in meta
        if (StringUtils.isNotEmpty(content)) {
            Document document = Jsoup.parse(content);
            Elements links = document.select("meta");
            for (Element link : links) {
                // 2.1、html4.01 <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
                String metaContent = link.attr("content");
                String metaCharset = link.attr("charset");
                if (metaContent.contains("charset")) {
                    metaContent = metaContent.substring(metaContent.indexOf("charset"), metaContent.length());
                    charset = metaContent.split("=")[1];
                    break;
                }
                // 2.2、html5 <meta charset="UTF-8" />
                else if (StringUtils.isNotEmpty(metaCharset)) {
                    charset = metaCharset;
                    break;
                }
            }
        }
        logger.debug("Auto get charset: {}", charset);
        // 3、todo use tools as cpdetector for content decode
        return charset;
    }
}
