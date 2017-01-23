package com.virjar.dungproxy.client.ippool.strategy.impl;

import java.util.Set;

import com.google.common.collect.Sets;
import com.virjar.dungproxy.client.ippool.strategy.ProxyDomainStrategy;
import org.apache.commons.lang3.StringUtils;

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
        // check host pattern
        if (StringUtils.startsWithIgnoreCase(host, "http") || StringUtils.contains(host, ":") || StringUtils.contains(host, "/")) {
            throw new IllegalArgumentException(host + " 不是一个合法域名,请注意填写的是域名,不是URL");
        }
        needIgnoreDomainList.add(host);
    }

    public void removeFromBackList(String host) {
        needIgnoreDomainList.remove(host);
    }
}
