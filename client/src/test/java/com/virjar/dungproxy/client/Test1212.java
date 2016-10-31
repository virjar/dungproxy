package com.virjar.dungproxy.client;

import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.client.ippool.IpPool;

import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by virjar on 16/10/31.
 */
public class Test1212 {
    public static void main(String[] args) {
        for (int i = 0; i < 15; i++) {
            try {
                System.out.println(HttpInvoker.get("http://1212.ip138.com/ic.asp", Charset.forName("gb2312")));
            } catch (IOException e) {
               // e.printStackTrace();
            }
        }
        IpPool.getInstance().destroy();
    }
}
