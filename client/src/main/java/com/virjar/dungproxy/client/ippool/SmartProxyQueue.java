package com.virjar.dungproxy.client.ippool;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.virjar.dungproxy.client.model.AvProxy;

/**
 * 智能的代理管理容器。他是单独为代理IP这种打分模型设计的容器<br/>
 * Created by virjar on 16/11/15.
 */
public class SmartProxyQueue {
    private static final Logger logger = LoggerFactory.getLogger(SmartProxyQueue.class);
    private double ratio;

    // ip使用时间间隔,如果被调度到的时候,发现IP使用时间太短,则先放弃本IP的调度
    private long useInterval = 0L;

    private final ReentrantLock mutex = new ReentrantLock();

    // 效率不高,但是先用这个实现.我们使用两个数据结构来实现绑定IP和优先级IP两种方案
    private LinkedList<AvProxy> proxies = Lists.newLinkedList();
    private Set<AvProxy> consistentBuckets = Sets.newConcurrentHashSet();

    // 暂时封禁的容器,放到本容器的IP只是被暂时封禁,但是不会被下线
    private LinkedList<AvProxy> blockedProxies = Lists.newLinkedList();

    public SmartProxyQueue(double ratio, long useInterval) {
        if (ratio < 0 || ratio > 1) {
            throw new IllegalStateException("ratio for SmartProxyQueue need between 0 and 1");
        }
        // 暂时不考虑多个domain的差异化配置
        this.useInterval = useInterval;
        this.ratio = ratio;
    }

    /**
     * 一般用在初始化的时候,向容器中增加代理
     *
     * @param avProxies
     */
    public void addAllProxy(Collection<AvProxy> avProxies) {
        for (AvProxy avProxy : avProxies) {
            addWithScore(avProxy);// 直接按照分值插入,主要考IP池自动调整优先级,或者上层通过分值控制初始值,没必要把这个逻辑做得很复杂
        }
    }

    public void addWithScore(AvProxy avProxy) {
        checkScore(avProxy.getAvgScore());
        mutex.lock();
        try {
            if (consistentBuckets.contains(avProxy)) {
                return;
            }
            int index = (int) (proxies.size() * (ratio + (1 - ratio) * (1 - avProxy.getAvgScore())));
            proxies.add(index, avProxy);
            consistentBuckets.add(avProxy);
        } finally {
            mutex.unlock();
        }

    }

    /**
     *
     * @param adjustTail 是否调整到队列尾部,这个时候使用的是整体空间的轮询模型,而不是局部轮询。主要适用IP数量较少的时候,全部轮询
     * @return
     */
    public AvProxy getAndAdjustPriority(boolean adjustTail) {
        mutex.lock();
        boolean hasBlock = false;
        try {
            for (;;) {
                AvProxy poll = proxies.poll();
                if (poll == null) {
                    return null;
                }
                /**
                 * 云代理也要封禁,考虑云代理的并发数
                 */
                if (System.currentTimeMillis() - poll.getLastUsedTime() < useInterval) {
                    hasBlock = true;
                    blockedProxies.addLast(poll);// 使用频率太高,放到备用资源池
                    logger.info("IP:{}使用小于规定时间间隔{}秒,暂时封禁", poll.getIp(), (useInterval / 1000));
                    continue;
                }
                int index = proxies.size() - 1;
                if (!adjustTail) {
                    index = (int) (proxies.size() * ratio);
                }
                proxies.add(index, poll);
                return poll;
            }
        } finally {
            mutex.unlock();
            if (hasBlock) {// 为了避免多次调用这个方法,考虑性能问题,虽然这个方法不消耗性能
                recoveryBlockedProxy();
            }

        }
    }

    /**
     * 解禁因为频率问题被block的IP资源
     */
    public void recoveryBlockedProxy() {
        mutex.lock();
        try {
            if (blockedProxies.size() == 0) {
                return;
            }
            int recoveredNumber = 0;
            Iterator<AvProxy> iterator = blockedProxies.iterator();
            while (iterator.hasNext()) {
                AvProxy next = iterator.next();
                if (System.currentTimeMillis() - next.getLastUsedTime() > useInterval) {
                    // proxies.add(0, next);
                    proxies.addFirst(next);// 封禁的前提是IP曾经在队列头部,处于最高优先级,所以这批资源直接恢复到头部
                    iterator.remove();
                    recoveredNumber++;
                }
            }
            logger.info("本次IP解禁数目为:{}", recoveredNumber);
        } finally {
            mutex.unlock();
        }
    }

    public void adjustPriority(AvProxy avProxy) {
        if (!consistentBuckets.contains(avProxy)) {// 如果已经下线,则不进行优先级调整动作
            return;
        }
        mutex.lock();
        try {
            if (!proxies.remove(avProxy)) {
                blockedProxies.remove(avProxy);
            }
            consistentBuckets.remove(avProxy);
            addWithScore(avProxy);
        } finally {
            mutex.unlock();
        }

    }

    public void offline(AvProxy avProxy) {
        mutex.lock();
        try {
            if (!proxies.remove(avProxy)) {
                blockedProxies.remove(avProxy);
            }
            consistentBuckets.remove(avProxy);
        } finally {
            mutex.unlock();
        }
    }

    public void offlineWithScore(double score) {
        checkScore(score);
        mutex.lock();
        try {
            AvProxy last = proxies.getLast();
            while (last != null && last.getAvgScore() < score) {
                last.offline();
                last = proxies.getLast();
            }

            last = blockedProxies.getLast();
            while (last != null && last.getAvgScore() < score) {
                last.offline();
                last = proxies.getLast();
            }
        } finally {
            mutex.unlock();
        }
    }

    private void checkScore(double score) {
        if (score < 0 || score > 1) {
            throw new IllegalStateException("avgScore for a AvProxy need between 0 and 1");
        }
    }

    public Iterator<? extends AvProxy> values() {
        return new ProxyQueueIterator();// 保证顺序
    }

    // 可能有bug,遍历的时候没有做到安全检查。暂时先这样吧
    private class ProxyQueueIterator implements Iterator<AvProxy> {
        Iterator<AvProxy> activedProxies = proxies.iterator();
        Iterator<AvProxy> blockedProxiesIterator = blockedProxies.iterator();
        boolean firstCollection = true;

        @Override
        public boolean hasNext() {
            if (!firstCollection) {
                return blockedProxiesIterator.hasNext();
            }
            if (activedProxies.hasNext()) {
                return true;
            }
            firstCollection = false;
            return blockedProxiesIterator.hasNext();
        }

        @Override
        public AvProxy next() {
            if (firstCollection) {
                return activedProxies.next();
            }
            return blockedProxiesIterator.next();
        }

        @Override
        public void remove() {
            if (firstCollection) {
                activedProxies.remove();
            } else {
                blockedProxiesIterator.remove();
            }
        }
    }

    /**
     * @return 可用IP量
     */
    public int availableSize() {
        return proxies.size();
    }

    /**
     *
     * @return 总IP量,包括当前正被封禁的IP量
     */
    public int allSize() {
        return proxies.size() + blockedProxies.size();
    }

    @Deprecated
    public AvProxy hint(int hash) {
        /*
         * if (consistentBuckets.size() == 0) { return null; } SortedMap<Integer, AvProxy> tmap =
         * this.consistentBuckets.tailMap(hash); return (tmap.isEmpty()) ? consistentBuckets.firstEntry().getValue() :
         * tmap.get(tmap.firstKey());
         */
        return null;
    }

    // 数据结构需要改造这个东西。。。我们需要一个高度灵活的优先级队列。但是不保证公平的优先级和绝对优先级。
    // ConcurrentLinkedQueue只是一个队列的时候。不能做到在队列中间插入数据z
    // private ConcurrentLinkedQueue<AvProxy> avProxies = new ConcurrentLinkedQueue<>();

    // monitor

    public double getRatio() {
        return ratio;
    }
}
