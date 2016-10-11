
package com.virjar.ipproxy.ippool.schedule;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.virjar.ipproxy.httpclient.HttpInvoker;
import com.virjar.ipproxy.ippool.DomainPool;
import com.virjar.ipproxy.ippool.config.Context;
import com.virjar.ipproxy.ippool.config.ObjectFactory;
import com.virjar.ipproxy.ippool.strategy.resource.ResourceFacade;
import com.virjar.ipproxy.util.CommonUtil;
import com.virjar.model.AvProxy;

/**
 * Description: 初始化时加载Proxy 定时收集Proxy<br/>
 * 一个工具类,离线跑代理IP数据,构建契合本地环境的代理IP数据
 * 
 * @author lingtong.fu
 * @version 2016-09-11 18:16
 */

public class Preheater {

    private static final Logger logger = LoggerFactory.getLogger(Preheater.class);

    private static Map<String, DomainPool> unSerialize() {
        Map<String, DomainPool> pool = Maps.newConcurrentMap();
        Map<String, List<AvProxy>> stringListMap = Context.getInstance().getAvProxyDumper().unSerializeProxy();
        if (stringListMap == null) {
            return pool;
        }
        String importer = Context.getInstance().getResourceFacade();
        for (Map.Entry<String, List<AvProxy>> entry : stringListMap.entrySet()) {
            if (pool.containsKey(entry.getKey())) {
                pool.get(entry.getKey()).addAvailable(entry.getValue());
            } else {
                pool.put(entry.getKey(), new DomainPool(entry.getKey(),
                        ObjectFactory.<ResourceFacade> newInstance(importer), entry.getValue()));
            }
        }
        return pool;
    }

    private static Map<String, List<AvProxy>> getPoolInfo(Map<String, DomainPool> pool) {
        return Maps.transformValues(pool, new Function<DomainPool, List<AvProxy>>() {
            @Override
            public List<AvProxy> apply(DomainPool domainPool) {// copy 一份新数据出去,数据结构会给外部使用,随意暴露可能会导致数据错误
                return Lists.transform(domainPool.availableProxy(), new Function<AvProxy, AvProxy>() {
                    @Override
                    public AvProxy apply(AvProxy input) {
                        return input.copy();
                    }
                });

            }
        });
    }

    public static void preheat(int expectedNumber, String url) {
        Map<String, DomainPool> stringDomainPoolMap = unSerialize();
        String domain = CommonUtil.extractDomain(url);
        DomainPool domainPool = stringDomainPoolMap.get(domain);
        if (domainPool == null) {
            String resourceFacade = Context.getInstance().getResourceFacade();
            domainPool = new DomainPool(domain, ObjectFactory.<ResourceFacade> newInstance(resourceFacade));
        }

        Set<AvProxy> proxySet = Sets.newHashSet();
        List<AvProxy> avProxies = domainPool.availableProxy();
        List<AvProxy> proxiesCopy = avProxies;
        while (proxySet.size() < expectedNumber) {
            for (AvProxy avProxy : avProxies) {
                for (int i = 0; i < 3; i++) {
                    try {
                        if (HttpInvoker.getStatus(url, avProxy.getIp(), avProxy.getPort()) == 200) {
                            proxySet.add(avProxy);
                            logger.info("valid proxy pass:{}", JSONObject.toJSONString(avProxy));
                            break;
                        }
                    } catch (IOException e) {
                        // do nothing
                    }
                }

            }
            avProxies = domainPool.getResourceFacade().importProxy(domain, url, 20);
        }

        // 下线代理池重不可用IP
        for (AvProxy avProxy : proxiesCopy) {
            if (!proxySet.contains(avProxy)) {
                avProxy.offline();
            }
        }
        domainPool.addAvailable(proxySet);
        Context.getInstance().getAvProxyDumper().serializeProxy(getPoolInfo(stringDomainPoolMap));
    }
}
