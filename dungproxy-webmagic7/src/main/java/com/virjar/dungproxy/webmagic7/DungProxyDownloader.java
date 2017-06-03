package com.virjar.dungproxy.webmagic7;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.virjar.dungproxy.client.ippool.config.ProxyConstant;
import com.virjar.dungproxy.client.util.PoolUtil;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.downloader.AbstractDownloader;
import us.codecraft.webmagic.downloader.HttpClientRequestContext;
import us.codecraft.webmagic.downloader.HttpUriRequestConverter;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.proxy.ProxyProvider;
import us.codecraft.webmagic.selector.PlainText;
import us.codecraft.webmagic.utils.CharsetUtils;
import us.codecraft.webmagic.utils.HttpClientUtils;

/**
 * The http downloader based on HttpClient.
 *
 * 为webMagic实现的downloader,如果有其他定制需求,参考本类实现即可<br/>
 * <br/>
 * 本类现在加入了很多新特性,不过使用方式和webmagic原生仍然兼容,如果是webmagic项目且需要定制downloader,建议在本类基础上做修改。
 * <ul>
 * <li>代理IP池特性(对接dungprxoy)</li>
 * <li>代理IP上下线特性</li>
 * <li>webmagic版本兼容(因为相对于webmagic,本类使用属于外来者,其代码逻辑不能跟随webmagic版本变动而变动)</li>
 * <li>放开downloader获取原生httpclient的口子,getHttpClient变成public方法,方便使用者模拟登录</li>
 * <li>多用户在线支持,使用multiUserCookieStore,天生支持多个用户并发的登录爬取数据</li>
 * <li>用户URL关系维护,他会自动纪录产生的新URL是那个user爬取到的,而且下次调度到一个新URL的时候,会自动获取到新URL是那个账户爬取到的,然后使用对应账户的cookie信息</li>
 * </ul>
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
 * @since 0.0.1
 */
@ThreadSafe
public class DungProxyDownloader extends AbstractDownloader {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, CloseableHttpClient> httpClients = new HashMap<String, CloseableHttpClient>();

    private DungProxyHttpClientGenerator httpClientGenerator = new DungProxyHttpClientGenerator();

    private HttpUriRequestConverter httpUriRequestConverter = new HttpUriRequestConverter();

    private ProxyProvider proxyProvider;

    private boolean responseHeader = true;

    public void setHttpUriRequestConverter(HttpUriRequestConverter httpUriRequestConverter) {
        this.httpUriRequestConverter = httpUriRequestConverter;
    }

    public void setProxyProvider(ProxyProvider proxyProvider) {
        this.proxyProvider = proxyProvider;
    }

    public CloseableHttpClient getHttpClient(Site site) {
        if (site == null) {
            return httpClientGenerator.getClient(null);
        }
        String domain = site.getDomain();
        CloseableHttpClient httpClient = httpClients.get(domain);
        if (httpClient == null) {
            synchronized (this) {
                httpClient = httpClients.get(domain);
                if (httpClient == null) {
                    httpClient = httpClientGenerator.getClient(site);
                    httpClients.put(domain, httpClient);
                }
            }
        }
        return httpClient;
    }

    @Override
    public Page download(Request request, Task task) {
        if (task == null || task.getSite() == null) {
            throw new NullPointerException("task or site can not be null");
        }
        logger.debug("downloading page {}", request.getUrl());
        CloseableHttpResponse httpResponse = null;
        CloseableHttpClient httpClient = getHttpClient(task.getSite());
        Proxy proxy = proxyProvider != null ? proxyProvider.getProxy(task) : null;
        HttpClientRequestContext requestContext = httpUriRequestConverter.convert(request, task.getSite(), proxy);
        // 扩展功能,支持多用户隔离,默认使用的是crawlerHttpClient,crawlerHttpClient默认则使用multiUserCookieStore
        if (request.getExtra(ProxyConstant.DUNGPROXY_USER_KEY) != null) {
            PoolUtil.bindUserKey(requestContext.getHttpClientContext(),
                    request.getExtra(ProxyConstant.DUNGPROXY_USER_KEY).toString());
        }

        Page page = UserSessionPage.fail();
        try {
            httpResponse = httpClient.execute(requestContext.getHttpUriRequest(),
                    requestContext.getHttpClientContext());
            page = handleResponse(request, task.getSite().getCharset(), httpResponse, task);
            if (needOfflineProxy(page)) {
                PoolUtil.offline(requestContext.getHttpClientContext());
                return addToCycleRetry(task.getSite(), page);
            }
            onSuccess(request);
            logger.debug("downloading page success {}", page);
            return page;
        } catch (IOException e) {
            if (needOfflineProxy(e)) {
                logger.warn("发生异常:{},IP下线");
                PoolUtil.offline(requestContext.getHttpClientContext());// 由IP异常导致,直接重试
                return addToCycleRetry(task.getSite(), null);
            }
            if (isLastRetry(request, task.getSite())) {// 移动异常日志位置,只记录最终失败的。中途失败不算失败
                logger.warn("download page {} error", request.getUrl(), e);
            }
            if (task.getSite() != null && task.getSite().getCycleRetryTimes() > 0) {
                return addToCycleRetry(task.getSite(), null);
            }

            onError(request);
            return page;
        } finally {
            if (httpResponse != null) {
                // ensure the connection is released back to pool
                EntityUtils.consumeQuietly(httpResponse.getEntity());
            }
            if (proxyProvider != null && proxy != null) {
                proxyProvider.returnProxy(proxy, page, task);
            }
        }
    }

    @Override
    public void setThread(int thread) {
        httpClientGenerator.setPoolSize(thread);
    }

    protected Page handleResponse(Request request, String charset, HttpResponse httpResponse, Task task)
            throws IOException {
        String content = getResponseContent(charset, httpResponse);
        Page page = new UserSessionPage();
        page.setRawText(content);
        page.setUrl(new PlainText(request.getUrl()));
        page.setRequest(request);
        page.setStatusCode(httpResponse.getStatusLine().getStatusCode());
        page.setDownloadSuccess(true);
        if (responseHeader) {
            page.setHeaders(HttpClientUtils.convertHeaders(httpResponse.getAllHeaders()));
        }
        return page;
    }

    private String getResponseContent(String charset, HttpResponse httpResponse) throws IOException {
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

    private String getHtmlCharset(HttpResponse httpResponse, byte[] contentBytes) throws IOException {
        return CharsetUtils.detectCharset(httpResponse.getEntity().getContentType().getValue(), contentBytes);
    }

    /**
     * 默认封禁403和401两个状态码的IP
     *
     * @param page 爬取结果
     * @return 是否需要封禁这个IP
     */
    protected boolean needOfflineProxy(Page page) {
        Integer statusCode = page.getStatusCode();
        return statusCode == 401 || statusCode == 403;// 401和403两个状态强制下线IP
    }

    protected boolean needOfflineProxy(IOException e) {
        return false;
    }

    /**
     * 0.7.x page里面自带statusCode,不需要单独提供这个方法
     * 
     * @param statusCode statusCode
     * @return
     */
    @Deprecated
    protected boolean needOfflineProxy(int statusCode) {
        throw new UnsupportedOperationException("");
    }

    /**
     * 判断当前请求是不是最后的重试,流程等同于 addToCycleRetry
     * 
     * @see us.codecraft.webmagic.Spider#doCycleRetry
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

    private Page addToCycleRetry(Site site, Page page) {
        if (site.getCycleRetryTimes() < 0) {
            site.setCycleRetryTimes(1);
        }
        if (page == null) {
            page = UserSessionPage.fail();
        } else {
            page.setDownloadSuccess(false);
        }
        return page;
    }
}
