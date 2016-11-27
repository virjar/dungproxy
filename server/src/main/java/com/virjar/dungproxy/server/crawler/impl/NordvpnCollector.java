package com.virjar.dungproxy.server.crawler.impl;

import java.util.List;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.virjar.dungproxy.server.crawler.AutoDownloadCollector;
import com.virjar.dungproxy.server.entity.Proxy;

/**
 * Created by virjar on 16/11/26.
 */
@Component
public class NordvpnCollector extends AutoDownloadCollector {
    private String lastUrl;

    public NordvpnCollector() {

    }

    @Override
    protected List<Proxy> parse(String response) {
        List<Proxy> ret = Lists.newArrayList();
        JSONArray jsonArray;
        try {
            jsonArray = JSON.parseArray(response);
        } catch (Exception e) {
            return ret;
        }
        for (Object o : jsonArray) {
            try {
                JSONObject jsonObject = (JSONObject) o;
                Proxy proxy = new Proxy();
                proxy.setIp(jsonObject.getString("ip"));
                proxy.setPort(NumberUtils.toInt(jsonObject.getString("port")));
                proxy.setAvailbelScore(0L);
                proxy.setSource(lastUrl);
                proxy.setConnectionScore(0L);
                ret.add(proxy);
            } catch (Exception e) {
                // do nothing
            }
        }
        return ret;
    }

    @Override
    protected String newUrl() {
        return "https://nordvpn.com/wp-admin/admin-ajax.php?searchParameters%5B0%5D%5Bname%5D=proxy-country&searchParameters%5B0%5D%5Bvalue%5D=&searchParameters%5B1%5D%5Bname%5D=proxy-ports&searchParameters%5B1%5D%5Bvalue%5D=&offset="
                + (pageNow * batchSize) + "&limit=" + batchSize + "&action=getProxies";
    }
}
