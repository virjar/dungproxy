package com.mantou.proxyservice.proxeservice.collector;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.virjar.dungproxy.server.crawler.TemplateBuilder;
import com.virjar.dungproxy.server.crawler.impl.TemplateCollector;
import com.virjar.dungproxy.server.entity.Proxy;

/**
 * Created by virjar on 16/11/26.
 */
public class IncloakTest {
    public static void main(String[] args) {
        List<TemplateCollector> templateCollectors = TemplateBuilder.buildfromSource("/handmapper_incloak.xml");
        TemplateCollector templateCollector = templateCollectors.get(0);
        List<Proxy> proxies = templateCollector.newProxy();
        System.out.println(JSONObject.toJSONString(proxies));
    }
}
