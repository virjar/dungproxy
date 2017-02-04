package com.virjar.dungproxy.client.ippool;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.virjar.dungproxy.client.ippool.config.DomainContext;
import com.virjar.dungproxy.client.ippool.config.DungProxyContext;
import com.virjar.dungproxy.client.ippool.strategy.ResourceFacade;
import com.virjar.dungproxy.client.model.AvProxy;
import com.virjar.dungproxy.client.model.AvProxyVO;
import com.virjar.dungproxy.client.util.IpAvValidator;

/**
 * Created by virjar on 16/9/29.
 */
public class DomainPool {
    private String domain;
    // 数据引入器,默认引入我们现在的服务器数据,可以扩展,改为其他数据来源
    private ResourceFacade resourceFacade;
    // 系统稳定的时候需要保持的资源
    private int coreSize = 50;
    private List<String> testUrls = Lists.newArrayList();

    private Random random = new Random(System.currentTimeMillis());

    private SmartProxyQueue smartProxyQueue;

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock(false);

    private List<AvProxy> removedProxies = Lists.newArrayList();

    // 备选的代理资源,他是通过IP下载器下载的的一批初始化IP,但是没有经过可用性测试
    private ConcurrentLinkedQueue<AvProxyVO> candidateProxies = new ConcurrentLinkedQueue<>();

    private AtomicBoolean isCandidateProxiesDownloading = new AtomicBoolean(false);

    private volatile long lastIpImportTimeStamp = 0L;

    private static final Logger logger = LoggerFactory.getLogger(DomainPool.class);

    private AtomicInteger refreshTaskNumber = new AtomicInteger(0);

    private DomainContext domainContext;

    private DungProxyContext dungProxyContext;

    public DomainContext getDomainContext() {
        return domainContext;
    }

    public DomainPool(String domain, DomainContext domainContext) {
        this(domain, domainContext, null);
    }

    public DomainPool(String domain, DomainContext domainContext, List<AvProxy> defaultProxy) {
        this.domain = domain;
        this.resourceFacade = domainContext.getResourceFacade();
        this.domainContext = domainContext;
        dungProxyContext = domainContext.getDungProxyContext();
        smartProxyQueue = new SmartProxyQueue(domainContext.getSmartProxyQueueRatio(), domainContext.getUseInterval());
        if (defaultProxy != null) {
            addAvailable(defaultProxy);
        }

        // for cloud proxy
        for (AvProxyVO cloudProxy : domainContext.getDungProxyContext().getCloudProxies()) {
            addAvailable(cloudProxy.toModel(this));
        }
    }

    public void addAvailable(Collection<AvProxy> avProxyList) {
        for (AvProxy avProxy : avProxyList) {
            avProxy.setDomainPool(this);// 注意考虑对象懒加载问题
            smartProxyQueue.addWithScore(avProxy);
        }
    }

    public void addAvailable(AvProxy avProxy) {
        avProxy.setDomainPool(this);
        smartProxyQueue.addWithScore(avProxy);
    }

    public List<AvProxy> availableProxy() {
        return Lists.newArrayList(smartProxyQueue.values());
    }

    public AvProxy bind(String url) {
        if (testUrls.size() < 10) {
            testUrls.add(url);
        } else {
            testUrls.set(random.nextInt(10), url);
        }
        if (needFresh()) {
            refresh();// 在新线程刷新
        }

        readWriteLock.readLock().lock();
        try {
            return smartProxyQueue.getAndAdjustPriority();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    /**
     * 当前IP池是否需要下载新的IP资源。
     * 
     * @return 是否
     */
    public boolean needFresh() {
        if (smartProxyQueue.availableSize() < coreSize) {
            smartProxyQueue.recoveryBlockedProxy();
        }
        return smartProxyQueue.availableSize() < coreSize;
    }

    /**
     * 动态调整IP刷新线程数量
     * 
     * @return 现在可以运行的线程数量
     */
    private int expectedRefreshTaskNumber() {

        if (smartProxyQueue.availableSize() >= coreSize) {
            return 0;
        }
        int threadNumber = (coreSize - smartProxyQueue.availableSize()) * 10 / coreSize;
        if (threadNumber == 0) {
            threadNumber = 1;
        }
        // logger.info("IP池可用IP数量:{} 当前准备进行刷新工作的线程数量:{}", smartProxyQueue.availableSize(), threadNumber);
        return threadNumber;
    }

    public void feedBack() {
        resourceFacade.feedBack(domain,
                Lists.transform(Lists.newArrayList(smartProxyQueue.values()), new Function<AvProxy, AvProxyVO>() {
                    @Override
                    public AvProxyVO apply(AvProxy input) {
                        return AvProxyVO.fromModel(input);
                    }
                }), Lists.transform(removedProxies, new Function<AvProxy, AvProxyVO>() {
                    @Override
                    public AvProxyVO apply(AvProxy input) {
                        return AvProxyVO.fromModel(input);
                    }
                }));
        removedProxies.clear();
    }

    /**
     * 本方法会启动线程异步刷线,所以不需要自己建立线程环境了,他不会检查可用IP是否足量,但是如果IP本身量太大的话,本调用也几乎无效(会检查是否需要下载IP,这个检查会失败),
     *
     */
    public void refresh() {
        if (testUrls.size() == 0) {
            return;// 数据还没有进来,不refresh
        }
        int expectedThreadNumber = expectedRefreshTaskNumber();
        if (refreshTaskNumber.get() > expectedThreadNumber) {
            // logger.info("当前刷新线程数:{} 大于调度线程数:{} 取消本次IP资源刷新任务", refreshTaskNumber.get(), expectedThreadNumber);
            return;
        }

        if (refreshTaskNumber.incrementAndGet() <= expectedThreadNumber) {
            Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        logger.info("IP资源刷新开始,当前刷新线程数量:{}...", refreshTaskNumber.get());
                        doRefresh();
                        logger.info("IP资源刷新结束...");
                    } finally {
                        refreshTaskNumber.decrementAndGet();
                    }
                }

            };
            thread.setDaemon(true);
            thread.start();
        }
    }

    private void checkAndExtendCandidateResource() {
        // 两分钟内下载过IP,则取消IP下载,因为一次IP下载本身可能需要十几秒
        if (System.currentTimeMillis() - lastIpImportTimeStamp < 120000) {
            return;
        }
        // 候选IP足量,取消IP下载
        if (candidateProxies.size() + smartProxyQueue.availableSize() > (coreSize * 1.5)) {
            return;
        }
        // download new proxies
        // 同一个时刻只能有一个线程进行IP下载
        if (isCandidateProxiesDownloading.compareAndSet(false, true)) {
            try {
                List<AvProxyVO> avProxies = resourceFacade.importProxy(domain,
                        testUrls.get(random.nextInt(testUrls.size())), coreSize);
                logger.info("在线IP刷新,当前下载到的IP数目为:{}", avProxies.size());
                candidateProxies.addAll(avProxies);
            } finally {
                lastIpImportTimeStamp = System.currentTimeMillis();
                isCandidateProxiesDownloading.set(false);
            }
        }

    }

    private void doRefresh() {
        checkAndExtendCandidateResource();
        AvProxyVO avProxy;
        // PreHeater preHeater = dungProxyContext.getPreHeater();
        while ((avProxy = candidateProxies.poll()) != null) {
            if (IpAvValidator.available(avProxy, testUrls.get(random.nextInt(testUrls.size())))) {
                avProxy.setAvgScore(0.5);// 设置默认值。让他处于次级缓存的中间。
                addAvailable(avProxy.toModel(domainContext));
                logger.info("IP池当前可用IP数目:{}", smartProxyQueue.availableSize());
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        DomainPool that = (DomainPool) o;

        return domain.equals(that.domain);

    }

    @Override
    public int hashCode() {
        return (domain + "?/").hashCode();
    }

    public void offline(AvProxy avProxy) {
        smartProxyQueue.offline(avProxy);
        removedProxies.add(avProxy);
        if (avProxy.getReferCount() != 0) {
            logger.warn("IP offline {}", JSONObject.toJSONString(AvProxyVO.fromModel(avProxy)));
        }
    }

    public String getDomain() {
        return domain;
    }

    public ResourceFacade getResourceFacade() {
        return resourceFacade;
    }

    public void adjustPriority(AvProxy avProxy) {
        smartProxyQueue.adjustPriority(avProxy);
    }

    public int getCoreSize() {
        return coreSize;
    }

    public void setCoreSize(int coreSize) {
        this.coreSize = coreSize;
    }

    public List<String> getTestUrls() {
        return testUrls;
    }

    public SmartProxyQueue getSmartProxyQueue() {
        return smartProxyQueue;
    }

    public boolean isRefreshing() {
        return refreshTaskNumber.get() > 0;
    }
}
