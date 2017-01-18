package com.virjar.dungproxy.client.ippool;

import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private AtomicBoolean isRefreshing = new AtomicBoolean(false);

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
            if (smartProxyQueue.availableSize() == 0) {
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
        if (isRefreshing.get()) {
            return;
        }
        new Thread() {
            @Override
            public void run() {
                refresh();
            }
        }.start();
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
     * 快速刷新模型,当IP池IP容量严重不足的时候,进入快速刷新模式,这个时候每个域名不再有单线程限制,使得IP可以快速恢复
     * 
     * @return 是否启动快速刷新模式
     */
    private boolean needQuickRefresh() {
        boolean ret = smartProxyQueue.availableSize() * 3 < coreSize;
        if (ret) {
            logger.warn("IP池{} IP数量严重不足,进入快速刷新模式,期待IP可以快速恢复", domain);
        }
        return ret;
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
        if (isRefreshing.compareAndSet(false, true)) {
            try {
                doRefresh();
            } finally {
                isRefreshing.set(false);
            }
        } else if (needQuickRefresh()) {
            doRefresh();
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

    public boolean getIsRefreshing() {
        return isRefreshing.get();
    }
}
