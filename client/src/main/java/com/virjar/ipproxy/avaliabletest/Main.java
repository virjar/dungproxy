package com.virjar.ipproxy.avaliabletest;

import com.virjar.ipproxy.httpclient.CrawlerHttpClient;

/**
 * Created by virjar on 16/9/20.
 */
public class Main {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        String quatity = CrawlerHttpClient.getQuatity(
                "http://b2c.csair.com/B2C40/modules/bookingnew/main/flightSelectDirect.html?t=S",
                "59.108.201.235",
                80);
        System.out.println(quatity);
        System.out.println(System.currentTimeMillis() - start);
    }
}
