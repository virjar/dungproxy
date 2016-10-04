package com.virjar.ipproxy.avaliabletest;

import com.virjar.ipproxy.httpclient.HttpInvoker;
import com.virjar.ipproxy.ippool.IpPool;

/**
 * Created by virjar on 16/9/20.
 */
public class Main {
    public static void main(String[] args) {
        String quatity = HttpInvoker.getQuiet("http://pachong.org/anonymous.html");
        System.out.println(quatity);
        IpPool.getInstance().destroy();
    }
}
