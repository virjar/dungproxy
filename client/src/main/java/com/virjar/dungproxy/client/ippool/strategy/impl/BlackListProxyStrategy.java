package com.virjar.dungproxy.client.ippool.strategy.impl;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.virjar.dungproxy.client.ippool.strategy.ProxyDomainStrategy;

/**
 * Created by virjar on 16/9/30.
 */
public class BlackListProxyStrategy implements ProxyDomainStrategy {
    private Logger logger = LoggerFactory.getLogger(BlackListProxyStrategy.class);
    private Set<String> needIgnoreDomainList = Sets.newConcurrentHashSet();

    @Override
    public boolean needProxy(String host) {
        return !needIgnoreDomainList.contains(host);
    }

    public void add2BlackList(String host) {
        // check host pattern
        if (StringUtils.startsWithIgnoreCase(host, "http") || StringUtils.contains(host, ":")
                || StringUtils.contains(host, "/")) {
            throw new IllegalArgumentException(host + " 不是一个合法域名,请注意填写的是域名,不是URL");
        }
        needIgnoreDomainList.add(host);
    }

    public void addAllHost(String configRule) {
        if (StringUtils.isEmpty(configRule)) {
            logger.warn("您选择了黑名单代理策略,但是没有提供策略配置,代理池将不会代理任何请求");
            return;
        }
        for (String domain : Splitter.on(",").omitEmptyStrings().trimResults().split(configRule)) {
            add2BlackList(domain);
        }
    }

    public void removeFromBackList(String host) {
        needIgnoreDomainList.remove(host);
    }
}
