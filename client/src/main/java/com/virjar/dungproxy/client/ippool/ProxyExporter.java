
package com.virjar.dungproxy.client.ippool;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.virjar.dungproxy.client.ippool.config.Context;
import com.virjar.dungproxy.client.ippool.config.ObjectFactory;
import com.virjar.dungproxy.client.ippool.strategy.ResourceFacade;
import com.virjar.dungproxy.client.model.AvProxy;
import com.virjar.dungproxy.client.model.AvProxyVO;
import com.virjar.dungproxy.client.util.CommonUtil;
import com.virjar.dungproxy.client.util.IpAvValidator;

/**
 * Description: 初始化时加载Proxy 定时收集Proxy<br/>
 * 一个工具类,离线跑代理IP数据,构建契合本地环境的代理IP数据
 *
 * @author lingtong.fu
 * @version 2016-09-11 18:16
 */

public class ProxyExporter {

    private static final Logger logger = LoggerFactory.getLogger(ProxyExporter.class);
    private Set<String> taskUrls = Sets.newConcurrentHashSet();
    private static int threadNumber = 200;// TODO 配置这个参数
    private ExecutorService pool;
    private AtomicBoolean hasInit = new AtomicBoolean(false);
    private Map<String, DomainPool> stringDomainPoolMap;
    private AtomicLong processedTask = new AtomicLong(0);
    private AtomicBoolean isRunning = new AtomicBoolean(false);

    /**
     * 提供一个主函数,用于在jar包调用preHeater模块,为数据真正可用做准备
     */
    public static void main(String[] args) {
        start();
    }

    public static void start() {
        List<String> preHeaterTaskList = Context.getInstance().getPreHeaterTaskList();
        ProxyExporter preHeater = new ProxyExporter();
        for (String url : preHeaterTaskList) {
            preHeater.addTask(url);
        }
        preHeater.doPreHeat();
        preHeater.destroy();
    }

    private void init() {
        if (hasInit.compareAndSet(false, true)) {
            pool = Executors.newFixedThreadPool(threadNumber);
            isRunning.set(true);
            unSerialize();
        }
    }

    public void destroy() {
        isRunning.set(false);
        pool.shutdown();
    }

    public ProxyExporter addTask(String url) {
        taskUrls.add(url);
        return this;
    }

    public synchronized void doPreHeat() {
        if (taskUrls.size() == 0) {
            logger.warn("preHeater task is empty");
            return;
        }
        if (!hasInit.get()) {
            init();
        }
        Map<String, String> urlMap = transformDomainUrlMap(taskUrls);
        List<Future<Boolean>> futureList = Lists.newArrayList();
        ResourceFacade resourceFacade = ObjectFactory.newInstance(Context.getInstance().getResourceFacade());
        List<AvProxyVO> candidateProxies = resourceFacade.importProxy(null, null, 1000);
        // 加载历史数据
        int historyNum = 0;
        for (Map.Entry<String, DomainPool> entry : stringDomainPoolMap.entrySet()) {
            for (AvProxy avProxy : entry.getValue().availableProxy()) {
                futureList.add(pool.submit(new UrlCheckTask(avProxy, urlMap.get(entry.getKey()))));
                historyNum++;
            }
        }
        logger.info("load history proxy " + historyNum);
        // 加载服务器新导入的资源
        for (AvProxyVO avProxy : candidateProxies) {
            for (String url : taskUrls) {
                futureList.add(pool.submit(new UrlCheckTask(avProxy.toModel(), url)));
            }
        }
        CommonUtil.waitAllFutures(futureList);
        Context.getInstance().getAvProxyDumper().serializeProxy(
                Maps.transformValues(getPoolInfo(stringDomainPoolMap), new Function<List<AvProxy>, List<AvProxyVO>>() {
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

    private Map<String, String> transformDomainUrlMap(Collection<String> testUrls) {
        Map<String, String> ret = Maps.newHashMap();
        for (String url : testUrls) {
            ret.put(CommonUtil.extractDomain(url), url);
        }
        return ret;
    }

    /**
     * 运行时使用,IP再可用之前,先经历本校验
     * 
     * @param avProxy 代理
     * @param url 测试URL
     * @param domainPool 成功后放入的
     */
    public void check4UrlAsync(AvProxy avProxy, String url, DomainPool domainPool) {
        if (!hasInit.get()) {
            init();
        }
        pool.submit(new UrlCheckTask(domainPool, avProxy, url));

    }

    public boolean check4UrlSync(AvProxy avProxy, String url, DomainPool domainPool) {
        try {
            return new UrlCheckTask(domainPool, avProxy, url).call();
        } catch (Exception e) {
            return false;
        }
    }

    private class UrlCheckTask implements Callable<Boolean> {
        String url;
        AvProxy proxy;
        DomainPool domainPool;

        UrlCheckTask(AvProxy proxy, String url) {
            this.proxy = proxy.copy();
            this.url = url;
        }

        public UrlCheckTask(DomainPool domainPool, AvProxy proxy, String url) {
            this.domainPool = domainPool;
            this.proxy = proxy;
            this.url = url;
        }

        @Override
        public Boolean call() throws Exception {
            String domain = CommonUtil.extractDomain(url);
            if (domainPool == null) {
                domainPool = stringDomainPoolMap.get(domain);
            }
            if (domainPool == null) {
                domainPool = new DomainPool(domain,
                        ObjectFactory.<ResourceFacade> newInstance(Context.getInstance().getResourceFacade()));
                stringDomainPoolMap.put(domain, domainPool);
            }
            proxy.setDomainPool(domainPool);
            if (IpAvValidator.available(proxy, url)) {
                domainPool.addAvailable(Lists.newArrayList(proxy));
                logger.debug("preHeater available test passed for proxy:{} for url:{}", JSONObject.toJSONString(proxy),
                        url);
                return true;
            } else {
                proxy.offline();
                return false;

            }
        }
    }

    public ProxyExporter setThreadNumber(int threadNumber) {
        if (threadNumber < 1) {
            threadNumber = 1;
        }
        this.threadNumber = threadNumber;
        return this;
    }

    private void unSerialize() {
        Map<String, DomainPool> pool = Maps.newConcurrentMap();
        Map<String, List<AvProxyVO>> stringListMap = Context.getInstance().getAvProxyDumper().unSerializeProxy();
        if (stringListMap == null) {
            return;
        }
        String importer = Context.getInstance().getResourceFacade();
        for (Map.Entry<String, List<AvProxyVO>> entry : stringListMap.entrySet()) {
            List<AvProxy> avProxies = Lists.transform(entry.getValue(), new Function<AvProxyVO, AvProxy>() {
                @Override
                public AvProxy apply(AvProxyVO input) {
                    return input.toModel();
                }
            });

            if (pool.containsKey(entry.getKey())) {
                pool.get(entry.getKey()).addAvailable(avProxies);
            } else {
                pool.put(entry.getKey(), new DomainPool(entry.getKey(),
                        ObjectFactory.<ResourceFacade> newInstance(importer), avProxies));
            }
        }
        stringDomainPoolMap = pool;
    }

    private Map<String, List<AvProxy>> getPoolInfo(Map<String, DomainPool> pool) {
        return Maps.transformValues(pool, new Function<DomainPool, List<AvProxy>>() {
            @Override
            public List<AvProxy> apply(DomainPool domainPool) {// copy

                return domainPool.availableProxy(); // 一份新数据出去,数据结构会给外部使用,随意暴露可能会导致数据错误
                /*
                 * return Lists.transform(domainPool.availableProxy(), new Function<AvProxy, AvProxy>() {
                 * @Override public AvProxy apply(AvProxy input) { return input.copy(); } });
                 */

            }
        });
    }
}
