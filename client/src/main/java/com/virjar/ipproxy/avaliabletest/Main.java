package com.virjar.ipproxy.avaliabletest;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.BasicHttpContext;

import com.alibaba.fastjson.JSONObject;
import com.virjar.ipproxy.httpclient.HttpInvoker;
import com.virjar.ipproxy.ippool.IpPool;
import com.virjar.ipproxy.util.PoolUtil;
import com.virjar.model.AvProxy;

/**
 * Created by virjar on 16/9/20.
 */
public class Main {
    public static void main(String[] args) {
        HttpClientContext httpClientContext = HttpClientContext.adapt(new BasicHttpContext());

        for (int i = 0; i < 1000; i++) {
            String quatity = HttpInvoker.getQuiet("http://www.66ip.cn/3.html", httpClientContext);
            System.out.println(quatity);
            AvProxy bindProxy = PoolUtil.getBindProxy(httpClientContext);
            System.out.println(JSONObject.toJSONString(bindProxy));
            PoolUtil.cleanProxy(httpClientContext);
        }
        IpPool.getInstance().destroy();
    }
}
