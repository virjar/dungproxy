package com.virjar.ipproxy.avaliabletest;

import com.virjar.ipproxy.httpclient.CrawlerHttpClient;

/**
 * Created by virjar on 16/9/20.
 */
public class Main {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        String quatity = CrawlerHttpClient.getQuatity(
                "http://pachong.org/anonymous.html",
                "1.82.216.135",
                80);
        System.out.println(quatity);
        System.out.println(System.currentTimeMillis() - start);
    }
}
