package com.virjar.dungproxy.client;

import com.virjar.dungproxy.client.util.PoolUtil;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.BasicHttpContext;

import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.client.ippool.IpPool;

/**
 * Created by virjar on 16/10/30.
 */
public class ClientTest {
    public static void main(String[] args) {
        HttpClientContext httpClientContext = HttpClientContext.adapt(new BasicHttpContext());
        int faildTimes = 0;
        for (int i = 0; i < 10; i++) {
            PoolUtil.cleanProxy(httpClientContext);
            String quiet = HttpInvoker.get("http://www.66ip.cn/5.html", httpClientContext);
            if (quiet == null) {
                faildTimes++;
            }
        }
        System.out.println(faildTimes);
        IpPool.getInstance().destroy();
    }
}
