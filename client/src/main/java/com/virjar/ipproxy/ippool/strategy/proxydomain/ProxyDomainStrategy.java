package com.virjar.ipproxy.ippool.strategy.proxydomain;

/**
 * 需要被代理的网站 Created by virjar on 16/9/30.
 */
public interface ProxyDomainStrategy {
    public boolean needProxy(String host);
}
