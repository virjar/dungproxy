package com.virjar.dungproxy.server.crawler;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.BasicHttpContext;

import com.google.common.collect.Lists;
import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.client.util.PoolUtil;
import com.virjar.dungproxy.server.entity.Proxy;

/**
 * Created by virjar on 16/11/27.
 */
public abstract class AutoDownloadCollector extends NewCollector {
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
        if (totalFailedCount > 20) {
            pageNow = 0;
        }
        PoolUtil.cleanProxy(httpClientContext);
        for (int i = 0; i < 5; i++) {
            lastUrl = newUrl();
            String response = HttpInvoker.get(lastUrl, httpClientContext);
            if (StringUtils.isEmpty(response)) {
                totalFailedCount++;
                continue;
            }
            List<Proxy> parse = parse(response);
            if (parse.size() > 0) {
                totalFailedCount = 0;
                return parse;
            }
            totalFailedCount++;
        }
        return Lists.newArrayList();
    }

    protected abstract List<Proxy> parse(String response);

    protected abstract String newUrl();
}
