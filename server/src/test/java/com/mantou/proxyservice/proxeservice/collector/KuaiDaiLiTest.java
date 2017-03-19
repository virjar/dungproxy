package com.mantou.proxyservice.proxeservice.collector;

import java.io.IOException;
import java.util.List;

import org.dom4j.DocumentException;

import com.alibaba.fastjson.JSONObject;
import com.virjar.dungproxy.server.crawler.TemplateBuilder;
import com.virjar.dungproxy.server.crawler.impl.TemplateCollector;
import com.virjar.dungproxy.server.entity.Proxy;

/**
 * Created by virjar on 16/9/15.
 */
public class KuaiDaiLiTest {
    public static void main(String[] args) throws IOException, DocumentException {
        List<TemplateCollector> templateCollectors = TemplateBuilder.buildfromSource("/handmapper_kuaidaili.xml");
        TemplateCollector templateCollector = templateCollectors.get(0);
        List<Proxy> proxies = templateCollector.newProxy();
        System.out.println(JSONObject.toJSONString(proxies));
    }
}
