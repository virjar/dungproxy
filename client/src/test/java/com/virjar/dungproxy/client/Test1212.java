package com.virjar.dungproxy.client;

import java.nio.charset.Charset;

import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.client.ippool.IpPool;

/**
 * Created by virjar on 16/10/31.
 */
public class Test1212 {
    public static void main(String[] args) {
        for (int i = 0; i < 15; i++) {

            System.out.println(HttpInvoker.get("http://1212.ip138.com/ic.asp", Charset.forName("gb2312")));
        }
        IpPool.getInstance().destroy();
    }
}
