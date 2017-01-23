package com.virjar.dungproxy.client.ippool;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.virjar.dungproxy.client.ippool.config.Context;
import com.virjar.dungproxy.client.ippool.config.ObjectFactory;
import com.virjar.dungproxy.client.ippool.exception.PoolDestroyException;
import com.virjar.dungproxy.client.ippool.strategy.ProxyDomainStrategy;
import com.virjar.dungproxy.client.ippool.strategy.ResourceFacade;
import com.virjar.dungproxy.client.model.AvProxy;
import com.virjar.dungproxy.client.model.AvProxyVO;
import com.virjar.dungproxy.client.util.CommonUtil;

/**
 * Description: IpListPool
 *
 * @author lingtong.fu
 * @version 2016-09-03 01:13
 */
public class IpPool {

    private Logger logger = LoggerFactory.getLogger(IpPool.class);

    private Map<String, DomainPool> pool = Maps.newConcurrentMap();

    private volatile boolean isRunning = false;

    private GroupBindRouter groupBindRouter = new GroupBindRouter();

    private IpPool() {
        init();
    }

    private void init() {
        isRunning = true;
        unSerialize();

        groupBindRouter.buildCombinationRule(Context.getInstance().getRuleRouter());

        //反馈任务线程
        FeedBackThread feedBackThread = new FeedBackThread();
        feedBackThread.setDaemon(true);
        feedBackThread.start();

        //资源刷新线程,当前本任务意义不大了,因为资源刷新都是在实时计算和异步启动
        FreshResourceThread freshResourceThread = new FreshResourceThread();
        freshResourceThread.setDaemon(true);
        freshResourceThread.start();

    }

    private static IpPool instance = new IpPool();

    public static IpPool getInstance() {
        return instance;
    }

    public void destroy() {
        Context.getInstance().getAvProxyDumper()
                .serializeProxy(Maps.transformValues(getPoolInfo(), new Function<List<AvProxy>, List<AvProxyVO>>() {
                    @Override
                    public List<AvProxyVO> apply(List<AvProxy> input) {
                        return Lists.transform(input, new Function<AvProxy, AvProxyVO>() {
                            @Override
                            public AvProxyVO apply(AvProxy input) {
                                return AvProxyVO.fromModel(input);
                            }
                        });
                    }
                }));
        isRunning = false;
    }

    public void unSerialize() {
        Map<String, List<AvProxyVO>> stringListMap = Context.getInstance().getAvProxyDumper().unSerializeProxy();
        if (stringListMap == null) {
            return;
        }
        String importer = Context.getInstance().getResourceFacade();
        for (Map.Entry<String, List<AvProxyVO>> entry : stringListMap.entrySet()) {
            List<AvProxy> proxies = Lists.transform(entry.getValue(), new Function<AvProxyVO, AvProxy>() {

                @Override
                public AvProxy apply(AvProxyVO input) {
                    return input.toModel();
                }
            });
            if (pool.containsKey(entry.getKey())) {
                pool.get(entry.getKey()).addAvailable(proxies);
            } else {
                pool.put(entry.getKey(),
                        new DomainPool(entry.getKey(), ObjectFactory.<ResourceFacade> newInstance(importer)));
                pool.get(entry.getKey()).addAvailable(proxies);
            }
        }
    }

    public AvProxy bind(String host, String url) {
        if (!isRunning) {
            throw new PoolDestroyException();
        }
        host = groupBindRouter.routeDomain(host);

        ProxyDomainStrategy needProxyStrategy = Context.getInstance().getNeedProxyStrategy();
        if (!needProxyStrategy.needProxy(host)) {
            logger.info("域名:{}没有被代理", host);
            return null;
        }

        if (!pool.containsKey(host)) {
            synchronized (this) {
                if (!pool.containsKey(host)) {
                    String importer = Context.getInstance().getResourceFacade();
                    pool.put(host, new DomainPool(host, ObjectFactory.<ResourceFacade> newInstance(importer)));
                }
            }
        }
        AvProxy bind = pool.get(host).bind(url);
        if (bind == null) {
            logger.warn("IP池中,域名:{} 暂时没有IP", host);
        }
        return bind;
    }

    public Map<String, List<AvProxy>> getPoolInfo() {
        return Maps.transformValues(pool, new Function<DomainPool, List<AvProxy>>() {
            @Override
            public List<AvProxy> apply(DomainPool domainPool) {
                return domainPool.availableProxy();
            }
        });
    }

    // 向服务器反馈不可用IP
    private class FeedBackThread extends Thread {
        @Override
        public void run() {
            while (isRunning) {
                CommonUtil.sleep(Context.getInstance().getFeedBackDuration());
                for (DomainPool domainPool : pool.values()) {
                    try {
                        domainPool.feedBack();
                    } catch (Exception e) {
                        logger.error("ip feedBack error for domain:{}", domainPool.getDomain(), e);
                    }
                }
            }
        }
    }

    // 检查IP池资源是否足够,不够则启动线程下载资源
    private class FreshResourceThread extends Thread {
        @Override
        public void run() {
            while (isRunning) {
                for (DomainPool domainPool : pool.values()) {
                    try {
                        if (domainPool.needFresh()) {
                            domainPool.refresh();
                        }
                    } catch (Exception e) {
                        logger.error("error when refresh ip pool for domain:{}", domainPool.getDomain(), e);
                    }
                }
                CommonUtil.sleep(4000);
            }
        }
    }

    // for monitor
    public int totalDomain() {
        return pool.size();
    }

    public Map<String, DomainPool> getPool() {
        return pool;
    }
}
