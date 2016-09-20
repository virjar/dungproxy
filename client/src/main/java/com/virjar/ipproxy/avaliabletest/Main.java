package com.virjar.ipproxy.avaliabletest;

import com.virjar.ipproxy.httpclient.CrawlerHttpClient;

/**
 * Created by virjar on 16/9/20.
 */
public class Main {
    public static void main(String[] args) {
        String quatity = CrawlerHttpClient.getQuatity("http://www.66ip.cn/4.html", "122.72.18.160", 80);
        System.out.println(quatity);
    }
}
