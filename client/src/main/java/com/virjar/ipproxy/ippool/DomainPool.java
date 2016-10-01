package com.virjar.ipproxy.ippool;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.virjar.ipproxy.ippool.strategy.DefaultImporter;
import com.virjar.ipproxy.ippool.strategy.Importer;
import com.virjar.model.AvProxy;

/**
 * Created by virjar on 16/9/29.
 */
public class DomainPool {
    private String domain;
    // 数据引入器,默认引入我们现在的服务器数据,可以扩展,改为其他数据来源
    private Importer importer;
    // 系统稳定的时候需要保持的资源
    int coreSize = 10;
    // 系统可以运行的时候需要保持的资源数目,如果少于这个数据,系统将会延迟等待,直到资源load完成
    int minSize = 0;
    List<String> testUrls = Lists.newArrayList();

    private Random random = new Random(System.currentTimeMillis());

    private TreeMap<Integer, AvProxy> consistentBuckets = new TreeMap<>();

    private ReadWriteLock readWriteLock = new ReentrantReadWriteLock(false);

    private Map<Object, AvProxy> bindMap = Maps.newConcurrentMap();

    public DomainPool(String domain, Importer importer) {
        this.domain = domain;
        this.importer = importer;
        if (importer == null) {
            this.importer = new DefaultImporter();
        }
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
        try {
            AvProxy hint = hint(userID == null ? random.nextInt() : userID.hashCode());
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

    public void fresh() {
        new Thread("xixi") {
            @Override
            public void run() {
                List<AvProxy> avProxies = importer.importProxy(domain, testUrls.get(random.nextInt(10)));

                if (readWriteLock.writeLock().tryLock()) {
                    try {
                        for (AvProxy avProxy : avProxies) {
                            consistentBuckets.put(avProxy.hashCode(), avProxy);
                        }
                    } finally {
                        readWriteLock.writeLock().unlock();
                    }
                }
            }
        }.start();
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
}
