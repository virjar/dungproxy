package com.virjar.dungproxy.server.crawler.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.BasicHttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.client.util.CommonUtil;
import com.virjar.dungproxy.client.util.PoolUtil;
import com.virjar.dungproxy.server.crawler.NewCollector;
import com.virjar.dungproxy.server.crawler.URLGenerator;
import com.virjar.dungproxy.server.crawler.extractor.XmlModeFetcher;
import com.virjar.dungproxy.server.entity.Proxy;

/**
 * Created by virjar on 16/11/26.<br/>
 * 这个是模版收集器,不能被标记为spring bean
 * 
 * @see com.virjar.dungproxy.server.crawler.TemplateBuilder
 */
public class TemplateCollector extends NewCollector {
    private static final Logger logger = LoggerFactory.getLogger(TemplateCollector.class);
    private URLGenerator urlGenerator;

    private XmlModeFetcher fetcher;
    private String lasUrl;
    private int totalFaildCount = 0;
    private HttpClientContext httpClientContext = HttpClientContext.adapt(new BasicHttpContext());
    private String successKey;

    public XmlModeFetcher getFetcher() {
        return fetcher;
    }

    public void setFetcher(XmlModeFetcher fetcher) {
        this.fetcher = fetcher;
    }

    public URLGenerator getUrlGenerator() {
        return urlGenerator;
    }

    public void setUrlGenerator(URLGenerator urlGenerator) {
        this.urlGenerator = urlGenerator;
    }

    @Override
    public String lasUrl() {
        return lasUrl;
    }

    @Override
    public List<Proxy> doCollect() {
        int faileCount = 0;
        List<Proxy> ret = Lists.newArrayList();// 单次,使用同一个IP
        PoolUtil.cleanProxy(httpClientContext);

        while (ret.size() < this.batchSize) {
            if (totalFaildCount > 20 || faileCount > 20) {
                totalFaildCount = 0;
                urlGenerator.reset();
                break;
            }
            if (faileCount > 3) {// 连续3次失败,切换IP
                faileCount = 0;
                PoolUtil.cleanProxy(httpClientContext);
            }

            lasUrl = urlGenerator.newURL();
            String response = null;
            for (int i = 0; i < 3; i++) {
                logger.info("request url:{} failedCount", lasUrl, faileCount);
                response = HttpInvoker.get(lasUrl, httpClientContext);
                if (!StringUtils.isEmpty(response)) {
                    break;
                }
            }
            if (StringUtils.isEmpty(response)) {
                faileCount++;
                totalFaildCount++;
                continue;
            }
            if (!StringUtils.isEmpty(successKey) && !StringUtils.contains(response, successKey)) {
                PoolUtil.recordFailed(httpClientContext);
                faileCount++;
                totalFaildCount++;
                continue;
            }

            List<String> fetch = fetcher.fetch(response);
            if (fetch.size() == 0) {
                faileCount++;
                totalFaildCount++;
                continue;
            }
            ret.addAll(convert(fetch, lasUrl));
            faileCount = totalFaildCount = 0;
        }
        return ret;
    }

    public List<Proxy> convert(List<String> fetchResult, String source) {
        List<Proxy> ret = Lists.newArrayList();
        for (String str : fetchResult) {
            try {
                Proxy parseObject = JSONObject.parseObject(str, Proxy.class);
                if (!CommonUtil.isIPAddress(parseObject.getIp())) {
                    continue;
                }
                parseObject.setAvailbelScore(0L);
                parseObject.setConnectionScore(0L);
                parseObject.setSource(source);
                ret.add(parseObject);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return ret;
    }

    public void setSuccessKey(String successKey) {
        this.successKey = successKey;
    }
}
