package com.virjar.ipproxy.avaliabletest;

import com.virjar.ipproxy.httpclient.CrawlerHttpClient;

/**
 * Created by virjar on 16/9/20.
 */
public class Main {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        String quatity = CrawlerHttpClient.getQuatity(
                "http://proxy-list.org/english/index.php?p=2",
                "118.144.104.254",
                3128);
        System.out.println(quatity);
        System.out.println(System.currentTimeMillis() - start);
    }
}
