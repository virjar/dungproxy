package com.virjar.dungproxy.client;

import com.virjar.dungproxy.client.httpclient.HttpInvoker;

/**
 * Created by virjar on 16/11/22.
 */
public class Test66IP {
    public static void main(String[] args) {
        String s = HttpInvoker.get("http://www.baidu.com");
         s = HttpInvoker.get("http://www.baidu.com");
        System.out.println(s);
    }
}
