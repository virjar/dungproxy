package com.mantou.proxyservice.proxeservice.collector;

import com.alibaba.fastjson.JSONObject;
import com.virjar.dungproxy.server.crawler.TemplateBuilder;
import com.virjar.dungproxy.server.crawler.impl.TemplateCollector;
import com.virjar.dungproxy.server.entity.Proxy;

import java.util.List;

/**
 * Created by virjar on 16/11/27.
 */
public class XProxyTest {
    public static void main(String[] args) {
        TemplateCollector templateCollector = TemplateBuilder.buildfromSource("/handmapper_xproxy.xml").get(0);
        List<Proxy> proxies = templateCollector.doCollect();
        System.out.println(JSONObject.toJSONString(proxies));
    }
}
