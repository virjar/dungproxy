package com.mantou.proxyservice.proxeservice.collector;

import java.io.IOException;
import java.util.List;

import org.dom4j.DocumentException;

import com.alibaba.fastjson.JSONObject;
import com.virjar.crawler.Collector;
import com.virjar.entity.Proxy;

/**
 * Created by virjar on 16/9/15.
 */
public class ProxyListOrgTest {
    public static void main(String[] args) throws IOException, DocumentException {
        List<Collector> collectors = Collector.buildfromSource("/proxy-list_org.xml");
        List<Proxy> proxies = collectors.get(0).newProxy();
        System.out.println(JSONObject.toJSONString(proxies));
    }
}
