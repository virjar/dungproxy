package com.virjar.dungproxy.server.crawler;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.BasicHttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.client.util.PoolUtil;
import com.virjar.dungproxy.server.entity.Proxy;

/**
 * Created by virjar on 16/11/27.
 */
public abstract class AutoDownloadCollector extends NewCollector {
    private static final Logger logger = LoggerFactory.getLogger(AutoDownloadCollector.class);
    protected int pageNow = 0;
    private HttpClientContext httpClientContext = HttpClientContext.adapt(new BasicHttpContext());
    private int totalFailedCount = 0;

    private String lastUrl = null;

    @Override
    public String lasUrl() {
        return lastUrl;
    }

    @Override
    public List<Proxy> doCollect() {

        int failedCount = 0;
        List<Proxy> ret = Lists.newArrayList();// 单次,使用同一个IP
        PoolUtil.cleanProxy(httpClientContext);

        while (ret.size() < this.batchSize) {
            if (totalFailedCount > 20 || failedCount > 20) {
                totalFailedCount = 0;
                pageNow = 0;
                break;
            }
            if (failedCount > 3) {// 连续3次失败,切换IP
                failedCount = 0;
                PoolUtil.cleanProxy(httpClientContext);
            }

            lastUrl = newUrl();
            String response = null;
            for (int i = 0; i < 3; i++) {
                logger.info("request url:{} failedCount", lastUrl, totalFailedCount);
                response = HttpInvoker.get(lastUrl, httpClientContext);
                if (!StringUtils.isEmpty(response)) {
                    break;
                }
            }
            if (StringUtils.isEmpty(response)) {
                failedCount++;
                totalFailedCount++;
                continue;
            }

            List<Proxy> fetch = parse(response);
            if (fetch.size() == 0) {
                failedCount++;
                totalFailedCount++;
                continue;
            }
            ret.addAll(fetch);
            failedCount = totalFailedCount = 0;
        }
        return ret;
    }

    protected abstract List<Proxy> parse(String response);

    protected abstract String newUrl();
}
