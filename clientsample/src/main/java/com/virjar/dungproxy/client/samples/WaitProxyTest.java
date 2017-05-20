package com.virjar.dungproxy.client.samples;

import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.client.ippool.IpPoolHolder;
import com.virjar.dungproxy.client.ippool.config.DungProxyContext;

/**
 * Created by virjar on 17/5/20.
 */
public class WaitProxyTest {
    public static void main(String[] args) {
        // 开启代理IP池,设置IP池空阻塞等待
        DungProxyContext dungProxyContext = DungProxyContext.create().setWaitIfNoAvailableProxy(true)
                .setPoolEnabled(true);
        IpPoolHolder.init(dungProxyContext);

        for (int i = 0; i < 5; i++) {
            new Thread() {
                @Override
                public void run() {
                    for (int i = 0; i < 5; i++) {
                        String s = HttpInvoker.get("http://ip.cn/");
                        if (StringUtils.isEmpty(s)) {
                            continue;
                        }
                        Document parse = Jsoup.parse(s);
                        System.out.println(parse.select("#result").text());
                    }
                }
            }.start();
        }

        for (int i = 0; i < 10; i++) {
            String s = HttpInvoker.get("http://ip.cn/");
            if (StringUtils.isEmpty(s)) {
                continue;
            }
            Document parse = Jsoup.parse(s);
            System.out.println(parse.select("#result").text());
        }

    }
}
