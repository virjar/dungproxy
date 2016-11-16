package com.virjar.dungproxy.client.ippool.strategy.impl;

import java.util.Set;

import com.google.common.collect.Sets;
import com.virjar.dungproxy.client.ippool.strategy.ProxyDomainStrategy;

/**
 * Created by virjar on 16/9/30.
 */
public class WhiteListProxyStrategy implements ProxyDomainStrategy {
    private Set<String> whiteList = Sets.newConcurrentHashSet();

    @Override
    public boolean needProxy(String host) {
        return whiteList.contains(host);
    }

    public void addWhiteHost(String host) {
        whiteList.add(host);
    }

    public void removeFromWhiteHost(String host) {
        whiteList.remove(host);
    }
}
