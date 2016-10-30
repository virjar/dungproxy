package com.virjar.dungproxy.client;

import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.BasicHttpContext;

import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.client.ippool.IpPool;
import com.virjar.dungproxy.client.util.PoolUtil;

/**
 * Created by virjar on 16/10/30.
 */
public class ClientTest {
    public static void main(String[] args) {
        HttpClientContext httpClientContext = HttpClientContext.adapt(new BasicHttpContext());
        String quiet = HttpInvoker.getQuiet("http://www.66ip.cn/5.html", httpClientContext);
        System.out.println(JSONObject.toJSONString(PoolUtil.getBindProxy(httpClientContext)));
        System.out.println(quiet);
        IpPool.getInstance().destroy();
    }
}
