package com.mantou.proxyservice.proxeservice.collector;

import java.io.IOException;
import java.util.List;

import org.dom4j.DocumentException;

import com.alibaba.fastjson.JSONObject;
import com.virjar.crawler.Collector;
import com.virjar.entity.Proxy;
import com.virjar.ipproxy.ippool.IpPool;
import com.virjar.ipproxy.ippool.config.Context;

/**
 * Created by virjar on 16/9/15.
 */
public class GoubanjiaTest {
    public static void main(String[] args) throws IOException, DocumentException {
        List<Collector> collectors = Collector.buildfromSource("/proxy_goubanjia_com.xml");
        Collector collector = collectors.get(0);

        for (int i = 0; i < 30; i++) {
            List<Proxy> proxies = collector.newProxy();
            Context.getInstance().getAvProxyDumper().serializeProxy(IpPool.getInstance().getPoolInfo());
            System.out.println(JSONObject.toJSONString(proxies));
        }
        IpPool.getInstance().destroy();
    }
}
