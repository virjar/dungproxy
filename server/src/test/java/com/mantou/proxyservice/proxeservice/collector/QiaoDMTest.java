package com.mantou.proxyservice.proxeservice.collector;

import com.alibaba.fastjson.JSONObject;
import com.virjar.dungproxy.server.crawler.TemplateBuilder;
import com.virjar.dungproxy.server.crawler.impl.TemplateCollector;

/**
 * Created by virjar on 16/11/26.
 */
public class QiaoDMTest {
    public static void main(String[] args) {
        TemplateCollector templateCollector = TemplateBuilder.buildfromSource("/handmapper_qianDm.xml").get(0);
        System.out.println(JSONObject.toJSONString(templateCollector.newProxy()));
    }
}
