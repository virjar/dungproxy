package com.virjar.dungproxy.client.ippool.strategy.impl;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

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
        // check host pattern
        if (StringUtils.startsWithIgnoreCase(host, "http") || StringUtils.contains(host, ":") || StringUtils.contains(host, "/")) {
            throw new IllegalArgumentException(host + " 不是一个合法域名,请注意填写的是域名,不是URL");
        }
        whiteList.add(host);
    }

    public void removeFromWhiteHost(String host) {
        whiteList.remove(host);
    }
}
