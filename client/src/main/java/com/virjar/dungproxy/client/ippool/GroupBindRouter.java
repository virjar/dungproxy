package com.virjar.dungproxy.client.ippool;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Maps;

/**
 * Created by virjar on 17/1/14. <br/>
 * 一个网站可能存在多个域名,但是实际上他们可以使用同一个代理规则,通过本类对其进行路由
 */
public class GroupBindRouter {
    private Logger logger = LoggerFactory.getLogger(GroupBindRouter.class);
    private Map<String, Optional<String>> routeData = Maps.newConcurrentMap();
    private Map<String, String> routeRule = Maps.newConcurrentMap();

    public GroupBindRouter() {
        //buildRule("www.virjar.com:*");// 默认增加一个全部路由的策略,没有配置的
    }

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
            if (similarDomain.equals(keyDomain)) {
                continue;// 否则会打印一个无效日志,以及递归问题
            }
            routeRule.put(similarDomain, keyDomain);
        }
    }

    public void buildCombinationRule(String combinationRule) {
        if (StringUtils.isEmpty(combinationRule)) {
            return;
        }
        for (String rule : Splitter.on(";").split(combinationRule)) {
            buildRule(rule);
        }
    }

    /**
     * 支持通过正则的方式批量配置路由规则
     *
     * @param similarDomain 现在遇到的域名
     * @return
     */
    public String routeDomain(String similarDomain) {
        Optional<String> s = routeData.get(similarDomain);// 从缓存中加载规则
        if (s == null) {// 缓存没有命中,建立缓存数据
            synchronized (GroupBindRouter.class) {
                s = routeData.get(similarDomain);
                if (s == null) {
                    matchSimilarDomain(similarDomain);
                    s = routeData.get(similarDomain);
                }
            }
        }
        if (s.isPresent()) {// 缓存有数据,且有路由规则
            String routedDomain = s.get();
            logger.info("域名:{} 的代理规则路由到:{}", similarDomain, routedDomain);
            return routedDomain;
            // return routeDomain(routedDomain);
        }
        return similarDomain;// 缓存有数据,但是没有找到路由规则

    }

    /**
     * @param similarDomain 当前遇到的域名
     */
    private void matchSimilarDomain(String similarDomain) {
        for (String pattern : routeRule.keySet()) {
            if (pattern.equals(similarDomain) || similarDomain.matches(pattern)) {
                routeData.put(similarDomain, Optional.of(routeRule.get(pattern)));
                return;
            }
        }
        // 所有规则都检查过,没有找到对应的路由规则
        routeData.put(similarDomain, Optional.<String> absent());
    }

    public int ruleSize(){
        return routeRule.size();
    }
}
