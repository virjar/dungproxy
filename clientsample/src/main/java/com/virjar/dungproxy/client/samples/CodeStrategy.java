package com.virjar.dungproxy.client.samples;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.virjar.dungproxy.client.httpclient.DunProxyHttpRequestRetryHandler;
import com.virjar.dungproxy.client.httpclient.conn.ProxyBindRoutPlanner;
import com.virjar.dungproxy.client.ippool.IpPoolHolder;
import com.virjar.dungproxy.client.ippool.config.DungProxyContext;
import com.virjar.dungproxy.client.ippool.strategy.impl.JSONFileAvProxyDumper;
import com.virjar.dungproxy.client.ippool.strategy.impl.WhiteListProxyStrategy;

/**
 * Created by virjar on 17/1/28.
 */
public class CodeStrategy {
    public static void main(String[] args) throws IOException {
        // Step1 代理策略,确定那些请求将会被代理池代理
        WhiteListProxyStrategy whiteListProxyStrategy = new WhiteListProxyStrategy();
        whiteListProxyStrategy.addAllHost("58.com");

        //确定缓存文件位置,如果没有预热,可以不指定
        JSONFileAvProxyDumper jsonFileAvProxyDumper = new JSONFileAvProxyDumper();
        jsonFileAvProxyDumper.setDumpFileName("你的IP文件位置");
        // Step2 创建并定制代理规则
        DungProxyContext dungProxyContext = DungProxyContext.create().setNeedProxyStrategy(whiteListProxyStrategy)
                .setAvProxyDumper(jsonFileAvProxyDumper);

        dungProxyContext.getGroupBindRouter().buildCombinationRule("58.com:.*58.com");
        // Step3 使用代理规则初始化默认IP池
        IpPoolHolder.init(dungProxyContext);

        // step 4 将代理池注册到httpclient(两个为httpclient做的适配插件)
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
        httpClientBuilder.setRetryHandler(new DunProxyHttpRequestRetryHandler(null))
                .setRoutePlanner(new ProxyBindRoutPlanner());
        CloseableHttpClient closeableHttpClient = httpClientBuilder.build();

        HttpGet httpGet = new HttpGet("http://www.baidu.com");
        CloseableHttpResponse response = closeableHttpClient.execute(httpGet);

        String string = IOUtils.toString(response.getEntity().getContent());
        System.out.println(string);

    }
}
