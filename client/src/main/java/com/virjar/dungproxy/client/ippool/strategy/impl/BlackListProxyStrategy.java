package com.virjar.dungproxy.client.ippool.strategy.impl;

import java.util.Set;

import com.google.common.collect.Sets;
import com.virjar.dungproxy.client.ippool.strategy.ProxyDomainStrategy;

/**
 * Created by virjar on 16/9/30.
 */
public class BlackListProxyStrategy implements ProxyDomainStrategy {
    private Set<String> needIgnoreDomainList = Sets.newConcurrentHashSet();

    @Override
    public boolean needProxy(String host) {
        return !needIgnoreDomainList.contains(host);
    }

    public void add2BlackList(String host) {
        needIgnoreDomainList.add(host);
    }

    public void removeFromBackList(String host) {
        needIgnoreDomainList.remove(host);
    }
}
