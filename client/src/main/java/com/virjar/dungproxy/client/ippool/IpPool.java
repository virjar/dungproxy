package com.virjar.dungproxy.client.ippool;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.virjar.dungproxy.client.ippool.config.DungProxyContext;
import com.virjar.dungproxy.client.ippool.exception.PoolDestroyException;
import com.virjar.dungproxy.client.ippool.strategy.AvProxyDumper;
import com.virjar.dungproxy.client.ippool.strategy.ProxyDomainStrategy;
import com.virjar.dungproxy.client.ippool.strategy.impl.ProxyAllStrategy;
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

    private GroupBindRouter groupBindRouter;

    private DungProxyContext dungProxyContext;

    private AvProxyDumper avProxyDumper;

    private ProxyDomainStrategy proxyDomainStrategy;

    private IpPool(DungProxyContext dungProxyContext) {
        this.dungProxyContext = dungProxyContext;
        init();
    }

    private void init() {
        // step 1 load all component
        groupBindRouter = dungProxyContext.getGroupBindRouter();

        // 这是一个特殊逻辑,默认代理池只维护一个domain,在用户遇到需要爬取不同网站,切不通网站的代理IP需要区分的时候,针对于不通domain做配置的时候,才会取消这个特性
        if (proxyDomainStrategy instanceof ProxyAllStrategy && groupBindRouter.ruleSize() == 0) {
            groupBindRouter.buildRule("www.virjar.com:.*");
        }

        avProxyDumper = dungProxyContext.getAvProxyDumper();
        proxyDomainStrategy = dungProxyContext.getNeedProxyStrategy();
        isRunning = true;

        unSerialize();

        // 反馈任务线程
        FeedBackThread feedBackThread = new FeedBackThread();
        feedBackThread.setDaemon(true);
        feedBackThread.start();

        // 资源刷新线程,当前本任务意义不大了,因为资源刷新都是在实时计算和异步启动
        // FreshResourceThread freshResourceThread = new FreshResourceThread();
        // freshResourceThread.setDaemon(true);
        // freshResourceThread.start();

        // 如果系统执行结束,用户没有手动调用destroy,则由hook尝试自动调用,进行内存数据的dump
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                if (isRunning) {
                    IpPool.this.destroy();
                }
            }
        });
    }

    // private static IpPool instance = new IpPool();

    public static IpPool getInstance() {
        return IpPoolHolder.getIpPool();
    }

    public static IpPool create(DungProxyContext dungProxyContext) {
        dungProxyContext.handleConfig();
        return new IpPool(dungProxyContext);
    }

    public void destroy() {
        isRunning = false;
        avProxyDumper
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
    }

    public void unSerialize() {
        AvProxy.needRecordChange = false;// 设定标记位,取消序列化增量计数
        try {
            Map<String, List<AvProxyVO>> stringListMap = avProxyDumper.unSerializeProxy();
            if (stringListMap == null) {
                return;
            }
            for (final Map.Entry<String, List<AvProxyVO>> entry : stringListMap.entrySet()) {
                List<AvProxy> proxies = Lists.transform(entry.getValue(), new Function<AvProxyVO, AvProxy>() {

                    @Override
                    public AvProxy apply(AvProxyVO input) {
                        return input.toModel(dungProxyContext.genDomainContext(entry.getKey()));
                    }
                });
                if (pool.containsKey(entry.getKey())) {
                    pool.get(entry.getKey()).addAvailable(proxies);
                } else {
                    pool.put(entry.getKey(),
                            new DomainPool(entry.getKey(), dungProxyContext.genDomainContext(entry.getKey())));
                    pool.get(entry.getKey()).addAvailable(proxies);
                }
            }
        } finally {
            AvProxy.needRecordChange = true;
        }
    }

    public AvProxy bind(String host, String url) {
        if (!isRunning) {
            throw new PoolDestroyException();
        }
        host = groupBindRouter.routeDomain(host);

        if (!proxyDomainStrategy.needProxy(host)) {
            logger.info("域名:{}没有被代理", host);
            return null;
        }

        if (!pool.containsKey(host)) {
            synchronized (this) {
                if (!pool.containsKey(host)) {
                    pool.put(host, new DomainPool(host, dungProxyContext.genDomainContext(host)));
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
                CommonUtil.sleep(dungProxyContext.getFeedBackDuration());
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
