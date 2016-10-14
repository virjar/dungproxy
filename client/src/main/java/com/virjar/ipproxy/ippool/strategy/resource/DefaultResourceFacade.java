package com.virjar.ipproxy.ippool.strategy.resource;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.virjar.ipproxy.httpclient.HttpInvoker;
import com.virjar.model.AvProxy;
import com.virjar.model.FeedBackForm;

/**
 * Created by virjar on 16/9/29.
 */
public class DefaultResourceFacade implements ResourceFacade {
    private Logger logger = LoggerFactory.getLogger(DefaultResourceFacade.class);
    private String downloadSign = null;

    private static final String avUrl = "http://115.159.40.202:8080/proxyipcenter/av";
    private static final String feedBackUrl = "http://115.159.40.202:8080/proxyipcenter/feedBack";

    @Override
    public List<AvProxy> importProxy(String domain, String testUrl, Integer number) {
        if (number == null || number < 1) {
            number = 30;
        }
        List<NameValuePair> valuePairList = Lists.newArrayList();
        valuePairList.add(new BasicNameValuePair("usedSign", downloadSign));
        valuePairList.add(new BasicNameValuePair("checkUrl", testUrl));
        valuePairList.add(new BasicNameValuePair("domain", domain));
        valuePairList.add(new BasicNameValuePair("num", String.valueOf(number)));
        String response = HttpInvoker.postQuiet(avUrl, valuePairList);
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
        JSONArray jsonArray = jsonObject.getJSONArray("data");
        List<AvProxy> ret = Lists.newArrayList();
        for (Object obj : jsonArray) {
            JSONObject proxy = JSONObject.class.cast(obj);
            AvProxy avProxy = new AvProxy();
            avProxy.setIp(proxy.getString("ip"));
            avProxy.setPort(proxy.getInteger("port"));
            ret.add(avProxy);
        }
        return ret;
    }

    @Override
    public void feedBack(String domain, List<AvProxy> avProxies, List<AvProxy> disableProxies) {
        Preconditions.checkNotNull(domain);
        Preconditions.checkNotNull(avProxies);
        Preconditions.checkNotNull(disableProxies);
        FeedBackForm feedBackForm = new FeedBackForm();
        feedBackForm.setDomain(domain);
        feedBackForm.setAvProxy(avProxies);
        feedBackForm.setDisableProxy(disableProxies);
        HttpInvoker.postQuiet(feedBackUrl, feedBackForm);
    }
}
