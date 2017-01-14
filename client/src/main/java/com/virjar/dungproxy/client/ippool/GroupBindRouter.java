package com.virjar.dungproxy.client.ippool;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

/**
 * Created by virjar on 17/1/14. <br/>
 * 一个网站可能存在多个域名,但是实际上他们可以使用同一个代理规则,通过本类对其进行路由
 */
public class GroupBindRouter {
    private Logger logger = LoggerFactory.getLogger(GroupBindRouter.class);
    private Map<String, String> routeData = Maps.newConcurrentMap();

    /**
     * domain:similarDomain1,similarDomain2,similarDomain3,similarDomain4...<br/>
     * 加载路由规则
     *
     * @param rule 规则文本
     */
    public void buildRule(String rule) {
        if (!StringUtils.contains(rule, ":")) {
            throw new IllegalArgumentException(
                    "domain:similarDomain1,similarDomain2,similarDomain3,similarDomain4  \":\" is lost");
        }
        String[] split = rule.split(":");
        String keyDomain = split[0];
        String similarDomainList = split[1];

        for (String similarDomain : Splitter.on(",").split(similarDomainList)) {
            routeData.put(similarDomain, keyDomain);
        }
    }

    public void buildCombinationRule(String combinationRule) {
        if (StringUtils.isEmpty(combinationRule)) {
            return;
        }
        for (String rule : Splitter.on(";").split(combinationRule)
                ) {
            buildRule(rule);
        }
    }

    public String routeDomain(String similarDomain) {
        String s = routeData.get(similarDomain);
        if (s == null) {
            return similarDomain;
        } else {
            logger.info("域名:{} 的代理规则路由到:{}", similarDomain, s);
            return s;
        }
    }
}
