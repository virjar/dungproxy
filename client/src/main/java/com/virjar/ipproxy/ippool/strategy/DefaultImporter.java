package com.virjar.ipproxy.ippool.strategy;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.virjar.ipproxy.httpclient.CrawlerHttpClient;
import com.virjar.model.AvProxy;

/**
 * Created by virjar on 16/9/29.
 */
public class DefaultImporter implements Importer {
    private Logger logger = LoggerFactory.getLogger(DefaultImporter.class);
    private String downloadSign = null;

    private static final String avUrl = "http://115.159.40.202:8080/proxyipcenter/av?";

    @Override
    public List<AvProxy> importProxy(String domain, String testUrl) {
        List<NameValuePair> valuePairList = Lists.newArrayList();
        valuePairList.add(new BasicNameValuePair("usedSign", downloadSign));
        valuePairList.add(new BasicNameValuePair("checkUrl", testUrl));
        String url = avUrl + URLEncodedUtils.format(valuePairList, "utf-8");
        String response = CrawlerHttpClient.getQuatity(url);
        if (StringUtils.isBlank(response)) {
            logger.error("can not get available ip resource from server:{}", url);
            return Lists.newArrayList();
        }
        JSONObject jsonObject = JSONObject.parseObject(response);
        if (!jsonObject.getBoolean("status")) {
            logger.error("can not get available ip resource from server:{} last response is ", url, response);
            return Lists.newArrayList();
        }
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
}
