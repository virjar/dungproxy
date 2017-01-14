package com.virjar.dungproxy.client;

import com.virjar.dungproxy.client.ippool.PreHeater;
import org.apache.commons.lang.StringUtils;
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
        for (int i = 0; i < 30; i++) {
            new Thread(){
                @Override
                public void run() {
                    for (int j = 0; j <100 ; j++) {
                        String s = HttpInvoker.get("http://www.dytt8.net/index.html");
                       // System.out.println(s);
                    }
                }
            }.start();
        }
    }
    public static void main2(String[] args) {
        PreHeater.start();
    }
    public static void main1(String[] args) {
        HttpClientContext httpClientContext = HttpClientContext.adapt(new BasicHttpContext());
        int faildTimes = 0;

        for (int i = 0; i < 100; i++) {
            PoolUtil.cleanProxy(httpClientContext);
            String quiet = HttpInvoker.get("http://www.dytt8.net/index.html", httpClientContext);
            if(StringUtils.isNotEmpty(quiet)){
                System.out.println("访问成功");
            }else{
                System.out.println("访问失败");
            }
            AvProxy bindProxy = PoolUtil.getBindProxy(httpClientContext);
            if (bindProxy != null) {
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
