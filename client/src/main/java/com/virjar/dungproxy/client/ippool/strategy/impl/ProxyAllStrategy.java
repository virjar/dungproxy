package com.virjar.dungproxy.client.ippool.strategy.impl;

import com.virjar.dungproxy.client.ippool.strategy.ProxyDomainStrategy;

/**
 * Created by virjar on 17/2/10.
 */
public class ProxyAllStrategy implements ProxyDomainStrategy {
    @Override
    public boolean needProxy(String host) {
        return true;
    }
}
