package com.mantou.proxyservice.proxeservice.collector;

import com.alibaba.fastjson.JSONObject;
import com.virjar.crawler.Collector;
import com.virjar.entity.Proxy;
import org.dom4j.DocumentException;

import java.io.IOException;
import java.util.List;

/**
 * Created by virjar on 16/9/15.
 */
public class PachongOrgTest {
    public static void main(String[] args) throws IOException, DocumentException {
        List<Collector> collectors = Collector.buildfromSource("/pachong_org.xml");
        List<Proxy> proxies = collectors.get(0).newProxy(null);
        System.out.println(JSONObject.toJSONString(proxies));
    }
}
