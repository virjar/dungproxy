package com.virjar.dungproxy.client.ippool;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
    private int coreSize = 30;
    // 系统可以运行的时候需要保持的资源数目,如果少于这个数据,系统将会延迟等待,直到资源load完成
    private int minSize = 1;
    private List<String> testUrls = Lists.newArrayList();

    private Random random = new Random(System.currentTimeMillis());

    private SmartProxyQueue smartProxyQueue = new SmartProxyQueue();

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock(false);

    private Map<Object, AvProxy> bindMap = Maps.newConcurrentMap();

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

    public AvProxy bind(String url, Object userID) {
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

            AvProxy hint;
            if (userID == null) {
                hint = smartProxyQueue.getAndAdjustPriority();
            } else {
                hint = smartProxyQueue.hint(userID.hashCode());
            }
            if (userID != null && hint != null) {
                if (!hint.equals(bindMap.get(userID))) {
                    // IP 绑定改变事件
                }
                bindMap.put(userID, hint);
            }
            return hint;
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
     * @return 是否
     */
    public boolean needFresh() {
        if( smartProxyQueue.availableSize() < coreSize){
            smartProxyQueue.recoveryBlockedProxy();
        }
        return  smartProxyQueue.availableSize() < coreSize;
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

    public void refresh() {
        if (testUrls.size() == 0) {
            return;// 数据还没有进来,不refresh
        }
        if (isRefreshing.compareAndSet(false, true)) {
            try {
                List<AvProxyVO> avProxies = resourceFacade.importProxy(domain,
                        testUrls.get(random.nextInt(testUrls.size())), coreSize);
                PreHeater preHeater = Context.getInstance().getPreHeater();
                for (AvProxyVO avProxy : avProxies) {
                    if (preHeater.check4UrlSync(avProxy, testUrls.get(random.nextInt(testUrls.size())), this)) {
                        avProxy.setAvgScore(0.5);// 设置默认值。让他处于次级缓存的中间。
                        addAvailable(avProxy.toModel());
                    }
                }
            }finally {
                isRefreshing.set(false);
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
