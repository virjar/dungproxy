package com.virjar.ipproxy.avaliabletest;

import java.util.List;

import com.alibaba.fastjson.JSONObject;
import com.virjar.ipproxy.ippool.strategy.resource.DefaultResourceFacade;
import com.virjar.model.AvProxy;

/**
 * Created by virjar on 16/9/20.
 */
public class Main {
    public static void main(String[] args) {
        /*
         * HttpClientContext httpClientContext = HttpClientContext.adapt(new BasicHttpContext()); for (int i = 0; i <
         * 1000; i++) { String quatity = HttpInvoker.getQuiet("http://www.66ip.cn/3.html", httpClientContext);
         * System.out.println(quatity); AvProxy bindProxy = PoolUtil.getBindProxy(httpClientContext);
         * System.out.println(JSONObject.toJSONString(bindProxy)); PoolUtil.cleanProxy(httpClientContext); }
         * IpPool.getInstance().destroy();
         */
        DefaultResourceFacade defaultResourceFacade = new DefaultResourceFacade();
        List<AvProxy> avProxies = defaultResourceFacade.importProxy("www.66ip.cn", "http://www.66ip.cn/3.html", 10);
        System.out.println(JSONObject.toJSONString(avProxies));
        avProxies = defaultResourceFacade.importProxy("www.66ip.cn", "http://www.66ip.cn/3.html", 10);
        System.out.println(JSONObject.toJSONString(avProxies)); }
}
