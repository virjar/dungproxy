package com.virjar.dungproxy.client.ippool.strategy.impl;

import org.apache.commons.lang3.StringUtils;

import com.virjar.dungproxy.client.ippool.strategy.ProxyDomainStrategy;

/**
 * Created by virjar on 17/3/7.
 */
public class DomainGroupStrategy implements ProxyDomainStrategy {
    private String domainSuffix;

    public DomainGroupStrategy(String domainSuffix) {
        this.domainSuffix = domainSuffix;
    }

    @Override
    public boolean needProxy(String host) {
        return StringUtils.endsWithIgnoreCase(host, domainSuffix);
    }
}
