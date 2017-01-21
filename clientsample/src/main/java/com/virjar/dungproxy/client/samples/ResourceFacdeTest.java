package com.virjar.dungproxy.client.samples;

import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.google.common.collect.Lists;
import com.virjar.dungproxy.client.httpclient.HttpInvoker;

/**
 * Created by virjar on 17/1/21.
 */
public class ResourceFacdeTest {
    public static void main(String[] args) {
        // [{"name":"clientID","value":""},{"name":"checkUrl","value":""},{"name":"domain","value":""},{"name":"num","value":"500"}]
        String avUrl = "http://proxy.scumall.com:8080/proxyipcenter/av";
        List<NameValuePair> valuePairList = Lists.newArrayList();
        valuePairList.add(new BasicNameValuePair("clientID", "com.fosun.creepers"));

        valuePairList.add(
                new BasicNameValuePair("checkUrl", "http://www.creditchina.gov.cn/publicity_info_search?page=62210"));
        valuePairList.add(new BasicNameValuePair("domain", "www.creditchina.gov.cn"));
        valuePairList.add(new BasicNameValuePair("num", String.valueOf(500)));
        System.out.println("默认IP下载器,IP下载URL:" + avUrl);
        String response = HttpInvoker.post(avUrl, valuePairList);
        System.out.println(response);
    }
}
