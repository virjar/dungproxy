package com.virjar.dungproxy.client.ippool;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.virjar.dungproxy.client.ippool.config.Context;
import com.virjar.dungproxy.client.ippool.strategy.ResourceFacade;
import com.virjar.dungproxy.client.ippool.strategy.impl.DefaultResourceFacade;
import com.virjar.dungproxy.client.model.AvProxy;
import com.virjar.dungproxy.client.model.AvProxyVO;
import com.virjar.dungproxy.client.model.DefaultProxy;

/**
 * Created by virjar on 16/9/29.
 */
public class DomainPool {
    private String domain;
    // 数据引入器,默认引入我们现在的服务器数据,可以扩展,改为其他数据来源
    private ResourceFacade resourceFacade;
    // 系统稳定的时候需要保持的资源
    private int coreSize = 50;
    // 系统可以运行的时候需要保持的资源数目,如果少于这个数据,系统将会延迟等待,直到资源load完成
    private int minSize = 1;
    private List<String> testUrls = Lists.newArrayList();

    private Random random = new Random(System.currentTimeMillis());

    private SmartProxyQueue smartProxyQueue = new SmartProxyQueue();

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock(false);

    private List<AvProxy> removedProxies = Lists.newArrayList();

    private static final Logger logger = LoggerFactory.getLogger(DomainPool.class);

    private AtomicInteger refreshTaskNumber = new AtomicInteger(0);

    public DomainPool(String domain, ResourceFacade resourceFacade) {
        this(domain, resourceFacade, null);
    }

    public DomainPool(String domain, ResourceFacade resourceFacade, List<AvProxy> defaultProxy) {
        this.domain = domain;
        this.resourceFacade = resourceFacade;
        if (resourceFacade == null) {
            this.resourceFacade = new DefaultResourceFacade();
        }
        if (defaultProxy != null) {
            addAvailable(defaultProxy);
        }
    }

    public void addAvailable(Collection<AvProxy> avProxyList) {
        List<AvProxy> newList = Lists.newArrayList(avProxyList);// 傻逼Collection可能是懒加载的。导致遍历的对象是新建的,操作不到
        for (AvProxy avProxy : newList) {// 这里必须设置这个,然后才能真正放到资源池里面去
            avProxy.setDomainPool(this);
        }
        smartProxyQueue.addAllProxy(newList);
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
            refreshInNewThread();// 在新线程刷新
        }

        readWriteLock.readLock().lock();
        try {
            if (smartProxyQueue.availableSize() == 0) {// TODO 移除这个逻辑,统一服务也要参与竞争,也把它放到IP池里面
                List<DefaultProxy> defaultProxyList = Context.getInstance().getDefaultProxyList();
                if (defaultProxyList.size() == 0) {
                    return null;
                }
                return defaultProxyList.get(new Random().nextInt(defaultProxyList.size()));
            }
            return smartProxyQueue.getAndAdjustPriority();
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    private void refreshInNewThread() {

        Thread thread = new Thread() {
            @Override
            public void run() {
                refresh();
            }
        };
        thread.setDaemon(true);
        thread.start();
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
        logger.info("IP池可用IP数量:{} 当前准备进行刷新工作的线程数量:{}", smartProxyQueue.availableSize(), threadNumber);
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
     * 是一个耗时任务,上层调用需要自己把本动作放在一个线程,当然本方法线程安全
     */
    public void refresh() {
        if (testUrls.size() == 0) {
            return;// 数据还没有进来,不refresh
        }
        int expectedThreadNumber = expectedRefreshTaskNumber();
        if (refreshTaskNumber.get() > expectedThreadNumber) {
            logger.info("当前刷新线程数:{} 大于调度线程数:{} 取消本次IP资源刷新任务", refreshTaskNumber.get(), expectedThreadNumber);
            return;
        }

        if (refreshTaskNumber.incrementAndGet() <= expectedThreadNumber) {
            try {
                logger.info("IP资源刷新开始...");
                doRefresh();
                logger.info("IP资源刷新结束...");
            } finally {
                refreshTaskNumber.decrementAndGet();
            }
        }
    }

    private void doRefresh() {
        List<AvProxyVO> avProxies = resourceFacade.importProxy(domain, testUrls.get(random.nextInt(testUrls.size())),
                coreSize);
        PreHeater preHeater = Context.getInstance().getPreHeater();
        for (AvProxyVO avProxy : avProxies) {
            if (preHeater.check4UrlSync(avProxy, testUrls.get(random.nextInt(testUrls.size())), this)) {
                avProxy.setAvgScore(0.5);// 设置默认值。让他处于次级缓存的中间。
                addAvailable(avProxy.toModel());
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

    public int getMinSize() {
        return minSize;
    }

    public void setMinSize(int minSize) {
        this.minSize = minSize;
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
