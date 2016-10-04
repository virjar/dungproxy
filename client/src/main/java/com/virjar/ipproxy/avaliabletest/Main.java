package com.virjar.ipproxy.avaliabletest;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.BasicHttpContext;

import com.alibaba.fastjson.JSONObject;
import com.virjar.ipproxy.httpclient.HttpInvoker;
import com.virjar.ipproxy.ippool.IpPool;
import com.virjar.ipproxy.ippool.config.ProxyConstant;
import com.virjar.ipproxy.util.PoolUtil;
import com.virjar.model.AvProxy;

/**
 * Created by virjar on 16/9/20.
 */
public class Main {
    public static void main(String[] args) {
        HttpClientContext httpClientContext = HttpClientContext.adapt(new BasicHttpContext());

        for (int i = 0; i < 1000; i++) {
            String quatity = HttpInvoker.getQuiet("http://pachong.org/anonymous.html", httpClientContext);
            AvProxy attribute = httpClientContext.getAttribute(ProxyConstant.USED_PROXY_KEY, AvProxy.class);
            if (attribute != null) {
                if (StringUtils.contains(quatity,
                        "<meta http-equiv=\"refresh\" content=\"0.1;url=http://www.linktom.com/ba/ba.html\">")) {
                    PoolUtil.offline(httpClientContext);
                    System.out.println(quatity);
                }
                System.out.println(JSONObject.toJSONString(attribute));

            }
            PoolUtil.cleanProxy(httpClientContext);
        }
        IpPool.getInstance().destroy();
    }
}
