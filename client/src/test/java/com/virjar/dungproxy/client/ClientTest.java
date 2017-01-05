package com.virjar.dungproxy.client;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.BasicHttpContext;

import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.client.ippool.IpPool;
import com.virjar.dungproxy.client.model.AvProxy;
import com.virjar.dungproxy.client.util.CommonUtil;
import com.virjar.dungproxy.client.util.PoolUtil;

/**
 * Created by virjar on 16/10/30.
 */
public class ClientTest {
    public static void main(String[] args) {
        HttpClientContext httpClientContext = HttpClientContext.adapt(new BasicHttpContext());
        String quiet = HttpInvoker.get("http://www.xicidaili.com/nn/4", httpClientContext);


        int faildTimes = 0;
        for (int i = 0; i < 100; i++) {
            PoolUtil.cleanProxy(httpClientContext);
            quiet = HttpInvoker.get("http://www.xicidaili.com/nn/5", httpClientContext);
            System.out.println(quiet);
            AvProxy bindProxy = PoolUtil.getBindProxy(httpClientContext);
            if (bindProxy != null) {
                System.out.println(quiet);
            } else {
                CommonUtil.sleep(1000);
            }
            if (quiet == null) {
                faildTimes++;
            }
        }
        System.out.println(faildTimes);
        IpPool.getInstance().destroy();
    }
}
