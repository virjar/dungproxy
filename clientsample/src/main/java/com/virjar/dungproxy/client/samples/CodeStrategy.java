package com.virjar.dungproxy.client.samples;

import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.client.ippool.IpPoolHolder;
import com.virjar.dungproxy.client.ippool.config.DungProxyContext;
import com.virjar.dungproxy.client.ippool.strategy.impl.WhiteListProxyStrategy;

/**
 * Created by virjar on 17/1/28.
 */
public class CodeStrategy {
    public static void main(String[] args) {
        // Step1 代理策略,确定那些请求将会被代理池代理
        WhiteListProxyStrategy whiteListProxyStrategy = new WhiteListProxyStrategy();
        whiteListProxyStrategy.addAllHost("www.baidu.com");

        // Step2 创建并定制代理规则
        DungProxyContext dungProxyContext = DungProxyContext.create().setNeedProxyStrategy(whiteListProxyStrategy);

        // Step3 使用代理规则初始化默认IP池
        IpPoolHolder.init(dungProxyContext);

        // Step4 使用CrawlerHttpClient或者任何基于Httpclient插件植入IP池的方式调用IP池的API
        HttpInvoker.get("http://www.baidu.com");
    }
}
