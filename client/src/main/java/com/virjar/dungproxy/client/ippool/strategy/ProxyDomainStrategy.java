package com.virjar.dungproxy.client.ippool.strategy;

/**
 * 需要被代理的网站 Created by virjar on 16/9/30.
 */
public interface ProxyDomainStrategy {
    boolean needProxy(String host);
}
