package com.virjar.ipproxy.ippool;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.virjar.ipproxy.ippool.config.Context;
import com.virjar.ipproxy.ippool.strategy.resource.DefaultResourceFacade;
import com.virjar.ipproxy.ippool.strategy.resource.ResourceFacade;
import com.virjar.model.AvProxy;

/**
 * Created by virjar on 16/9/29.
 */
public class DomainPool {
    private String domain;
    // 数据引入器,默认引入我们现在的服务器数据,可以扩展,改为其他数据来源
    private ResourceFacade resourceFacade;
    // 系统稳定的时候需要保持的资源
    private int coreSize = 10;
    // 系统可以运行的时候需要保持的资源数目,如果少于这个数据,系统将会延迟等待,直到资源load完成
    private int minSize = 1;
    private List<String> testUrls = Lists.newArrayList();

    private Random random = new Random(System.currentTimeMillis());

    private TreeMap<Integer, AvProxy> consistentBuckets = new TreeMap<>();

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
        readWriteLock.writeLock().lock();
        try {
            for (AvProxy avProxy : avProxyList) {
                avProxy.setDomainPool(this);
                consistentBuckets.put(avProxy.hashCode(), avProxy);
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public List<AvProxy> availableProxy() {
        return Lists.newArrayList(consistentBuckets.values());
    }

    public AvProxy bind(String url, Object userID) {
        if (testUrls.size() < 10) {
            testUrls.add(url);
        } else {
            testUrls.set(random.nextInt(10), url);
        }
        if (consistentBuckets.size() < minSize) {
            fresh();
        }

        readWriteLock.readLock().lock();
        try {// 注意hash空间问题,之前是Integer,hash值就是字面值,导致hash空间只存在了正数空间
            AvProxy hint = hint(userID == null ? String.valueOf(random.nextInt()).hashCode() : userID.hashCode());
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

    public boolean needFresh() {
        return consistentBuckets.size() < coreSize;
    }

    public void feedBack() {
        resourceFacade.feedBack(domain, Lists.newArrayList(consistentBuckets.values()), removedProxies);
        removedProxies.clear();
        /*
         * for (AvProxy avProxy : consistentBuckets.values()) { avProxy.reset(); }
         */
    }

    public void fresh() {
        if (isRefreshing.compareAndSet(false, true)) {
            List<AvProxy> avProxies = resourceFacade.importProxy(domain, testUrls.get(random.nextInt(testUrls.size())),
                    coreSize);
            addAvailable(avProxies);
            PreHeater preHeater = Context.getInstance().getPreHeater();
            for (AvProxy avProxy : avProxies) {
                preHeater.check4Url(avProxy, testUrls.get(random.nextInt(testUrls.size())), this);
            }
            isRefreshing.set(false);
        }
    }

    /**
     * 分配一个IP,根据一致性哈希算法,这样根据业务ID可以每次绑定相同的IP
     * 
     * @param hash
     * @return
     */
    public AvProxy hint(int hash) {
        if (consistentBuckets.size() == 0) {
            return null;
        }
        SortedMap<Integer, AvProxy> tmap = this.consistentBuckets.tailMap(hash);
        return (tmap.isEmpty()) ? consistentBuckets.firstEntry().getValue() : tmap.get(tmap.firstKey());
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
        consistentBuckets.remove(avProxy.hashCode());
        removedProxies.add(avProxy);
        logger.warn("IP offline {}", JSONObject.toJSONString(avProxy));
    }

    public String getDomain() {
        return domain;
    }

    public ResourceFacade getResourceFacade() {
        return resourceFacade;
    }
}
