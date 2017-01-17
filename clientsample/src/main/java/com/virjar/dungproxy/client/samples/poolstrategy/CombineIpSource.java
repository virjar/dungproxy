package com.virjar.dungproxy.client.samples.poolstrategy;

import java.util.List;

import com.google.common.collect.Lists;
import com.virjar.dungproxy.client.ippool.strategy.ResourceFacade;
import com.virjar.dungproxy.client.ippool.strategy.impl.DefaultResourceFacade;
import com.virjar.dungproxy.client.model.AvProxyVO;

/**
 * Created by virjar on 17/1/17. <br/>
 * 联合资源导入器,同时支持导入自定义IP源和dungServer的数据源
 */
public class CombineIpSource implements ResourceFacade {
    List<ResourceFacade> delegate = Lists.newArrayList(new CombineIpSource(), new DefaultResourceFacade());

    @Override
    public List<AvProxyVO> importProxy(String domain, String testUrl, Integer number) {
        List<AvProxyVO> ret = Lists.newArrayList();
        for (ResourceFacade resourceFacade : delegate) {
            List<AvProxyVO> avProxyVOs = resourceFacade.importProxy(domain, testUrl, number);
            if (avProxyVOs != null) {
                ret.addAll(avProxyVOs);
            }
            if (ret.size() > number) {
                return ret;
            }
        }
        return ret;
    }

    @Override
    public void feedBack(String domain, List<AvProxyVO> avProxies, List<AvProxyVO> disableProxies) {
        for (ResourceFacade resourceFacade : delegate) {
            resourceFacade.feedBack(domain, avProxies, disableProxies);
        }
    }

    @Override
    public List<AvProxyVO> allAvailable() {
        for (ResourceFacade resourceFacade : delegate) {
            List<AvProxyVO> avProxyVOs = resourceFacade.allAvailable();
            if (avProxyVOs != null && avProxyVOs.size() > 0) {
                return avProxyVOs;
            }
        }
        return Lists.newArrayList();
    }
}
