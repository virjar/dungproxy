package com.virjar.dungproxy.client.ippool.strategy.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.protocol.HttpClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.client.httpclient.NameValuePairBuilder;
import com.virjar.dungproxy.client.ippool.strategy.ResourceFacade;
import com.virjar.dungproxy.client.model.AvProxyVO;
import com.virjar.dungproxy.client.model.FeedBackForm;
import com.virjar.dungproxy.client.util.PoolUtil;

/**
 * Created by virjar on 16/9/29.
 */
public class DefaultResourceFacade implements ResourceFacade {
    private Logger logger = LoggerFactory.getLogger(DefaultResourceFacade.class);
    private String downloadSign = null;
    private static String clientID = null;

    private static String avUrl = "http://proxy.scumall.com:8080/proxyipcenter/av";
    private static String feedBackUrl = "http://proxy.scumall.com:8080/proxyipcenter/feedBack";
    private static String allAvUrl = "http://proxy.scumall.com:8080/proxyipcenter/allAv";

    public static void setAvUrl(String avUrl) {
        DefaultResourceFacade.avUrl = avUrl;
    }

    public static void setFeedBackUrl(String feedBackUrl) {
        DefaultResourceFacade.feedBackUrl = feedBackUrl;
    }

    public static void setAllAvUrl(String allAvUrl) {
        DefaultResourceFacade.allAvUrl = allAvUrl;
    }

    public static void setClientID(String clientID) {
        DefaultResourceFacade.clientID = clientID;
    }

    @Override
    public List<AvProxyVO> importProxy(String domain, String testUrl, Integer number) {
        if (number == null || number < 1) {
            number = 30;
        }
        NameValuePairBuilder nameValuePairBuilder = NameValuePairBuilder.create();
        if (StringUtils.isNotEmpty(clientID)) {
            nameValuePairBuilder.addParam("clientID", clientID);
        } else {
            nameValuePairBuilder.addParam("usedSign", downloadSign);
        }
        nameValuePairBuilder.addParam("checkUrl", testUrl).addParam("domain", domain).addParam("num",
                String.valueOf(number));
        List<NameValuePair> valuePairList = nameValuePairBuilder.build();

        logger.info("默认IP下载器,IP下载URL:{}", avUrl);
        HttpClientContext httpClientContext = HttpClientContext.create();
        PoolUtil.disableDungProxy(httpClientContext);
        String response = HttpInvoker.post(avUrl, valuePairList, httpClientContext);

        if (StringUtils.isBlank(response)) {
            logger.error("can not get available ip resource from server: request body is {}",
                    JSONObject.toJSONString(valuePairList));
            return Lists.newArrayList();
        }
        JSONObject jsonObject = JSONObject.parseObject(response);
        if (!jsonObject.getBoolean("status")) {
            logger.error("can not get available ip resource from server:request  body is  last response is ",
                    JSONObject.toJSONString(valuePairList), response);
            return Lists.newArrayList();
        }
        jsonObject = jsonObject.getJSONObject("data");
        this.downloadSign = jsonObject.getString("sign");
        return convert(jsonObject.getJSONArray("data"));
    }

    @Override
    public void feedBack(String domain, List<AvProxyVO> avProxies, List<AvProxyVO> disableProxies) {
        Preconditions.checkNotNull(domain);
        Preconditions.checkNotNull(avProxies);
        Preconditions.checkNotNull(disableProxies);
        FeedBackForm feedBackForm = new FeedBackForm();
        feedBackForm.setDomain(domain);
        feedBackForm.setAvProxy(avProxies);
        feedBackForm.setDisableProxy(disableProxies);
        HttpClientContext httpClientContext = HttpClientContext.create();
        PoolUtil.disableDungProxy(httpClientContext);
        HttpInvoker.postJSON(feedBackUrl, feedBackForm,httpClientContext);
    }

    @Override
    public List<AvProxyVO> allAvailable() {
        logger.info("默认IP下载器,IP下载URL:{}", allAvUrl);
        HttpClientContext httpClientContext = HttpClientContext.create();
        PoolUtil.disableDungProxy(httpClientContext);
        String response = HttpInvoker.get(allAvUrl, httpClientContext);
        if (StringUtils.isBlank(response)) {
            logger.error("can not get available ip resource from server: ");
            return Lists.newArrayList();
        }
        JSONObject jsonObject = JSONObject.parseObject(response);
        if (!jsonObject.getBoolean("status")) {
            logger.error("can not get available ip resource from server:request  body is  last response is {} ",
                    response);
            return Lists.newArrayList();
        }
        return convert(jsonObject.getJSONArray("data"));
    }

    private List<AvProxyVO> convert(JSONArray jsonArray) {
        List<AvProxyVO> ret = Lists.newArrayList();
        for (Object obj : jsonArray) {
            JSONObject proxy = JSONObject.class.cast(obj);
            AvProxyVO avProxy = new AvProxyVO();
            avProxy.setIp(proxy.getString("ip"));
            avProxy.setPort(proxy.getInteger("port"));
            ret.add(avProxy);
        }
        return ret;
    }
}
