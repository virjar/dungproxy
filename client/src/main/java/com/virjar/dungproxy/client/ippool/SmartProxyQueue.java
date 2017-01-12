package com.virjar.dungproxy.client.ippool;

import java.util.*;

import com.virjar.dungproxy.client.ippool.config.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
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

    private final Object mutex = this;

    // 效率不高,但是先用这个实现.我们使用两个数据结构来实现绑定IP和优先级IP两种方案
    private LinkedList<AvProxy> proxies = Lists.newLinkedList();
    private TreeMap<Integer, AvProxy> consistentBuckets = new TreeMap<>();

    //暂时封禁的容器,放到本容器的IP只是被暂时封禁,但是不会被下线
    private LinkedList<AvProxy> blockedProxies = Lists.newLinkedList();

    public SmartProxyQueue() {
        this(0.3);
    }

    public SmartProxyQueue(double ratio) {
        if (ratio < 0 || ratio > 1) {
            throw new IllegalStateException("ratio for SmartProxyQueue need between 0 and 1");
        }
        //暂时不考虑多个domain的差异化配置
        this.useInterval = Context.getInstance().getGlobalProxyUseInterval();
        this.ratio = ratio;
    }

    /**
     * 一般用在初始化的时候,向容器中增加代理
     *
     * @param avProxies
     */
    public void addAllProxy(Collection<AvProxy> avProxies) {
        synchronized (mutex) {

            for (AvProxy avProxy : avProxies) {
                if (consistentBuckets.containsKey(avProxies.hashCode())) {
                    continue;
                }
                // Score score = avProxy.getScore();
                if (avProxy.getAvgScore() < 0D || avProxy.getAvgScore() > 1D) {
                    logger.warn("avProxy score is illegal , avgScore need between 0 and 1 ,real score is:{}",
                            avProxy.getAvgScore());
                    avProxy.setAvgScore(0.5D);// 数据损坏
                }

                if (proxies.size() == 0) {
                    proxies.add(avProxy);
                } else if (avProxy.getAvgScore() != 0.0) {// 考虑断点使用IP资源的时候。需要按照分值来插入
                    int index = (int) (proxies.size() * (ratio + (1 - ratio) * (1 - avProxy.getAvgScore())));
                    proxies.add(index, avProxy);
                } else {
                    proxies.addLast(avProxy);// 新加入资源,需要放置到链表尾部
                }
                consistentBuckets.put(avProxy.hashCode(), avProxy);
            }
        }

    }

    public void addWithScore(AvProxy avProxy) {
        checkScore(avProxy.getAvgScore());
        synchronized (mutex) {
            if (consistentBuckets.containsKey(avProxy.hashCode())) {
                return;
            }
            int index = (int) (proxies.size() * (ratio + (1 - ratio) * (1 - avProxy.getAvgScore())));
            proxies.add(index, avProxy);
            consistentBuckets.put(avProxy.hashCode(), avProxy);
        }

    }

    public AvProxy getAndAdjustPriority() {
        synchronized (mutex) {
            for (;;) {
                AvProxy poll = proxies.poll();
                if (poll == null) {
                    return null;
                }
                if (System.currentTimeMillis() - poll.getLastUsedTime() < useInterval) {
                    blockedProxies.add(poll);// 使用频率太高,放到备用资源池
                    continue;
                }
                int index = (int) (proxies.size() * ratio);
                proxies.add(index, poll);
                return poll;
            }
        }
    }

    /**
     * 解禁因为频率问题被block的IP资源
     */
    public void recoveryBlockedProxy() {
        synchronized (mutex) {
            if (blockedProxies.size() == 0) {
                return;
            }
            Iterator<AvProxy> iterator = blockedProxies.iterator();
            while (iterator.hasNext()) {
                AvProxy next = iterator.next();
                if (System.currentTimeMillis() - next.getLastUsedTime() > useInterval) {
                    proxies.add(0, next);// 封禁的前提是IP曾经在队列头部,处于最高优先级,所以这批资源直接恢复到头部
                    iterator.remove();
                }
            }
        }
    }

    public void adjustPriority(AvProxy avProxy) {
        if (!consistentBuckets.containsKey(avProxy.hashCode())) {// 如果已经下线,则不进行优先级调整动作
            return;
        }
        synchronized (mutex) {
            proxies.remove(avProxy);
            consistentBuckets.remove(avProxy.hashCode());
        }
        addWithScore(avProxy);
    }

    public void offline(AvProxy avProxy) {
        synchronized (mutex) {
            proxies.remove(avProxy);
            consistentBuckets.remove(avProxy.hashCode());
        }
    }

    public void offlineWithScore(double score) {
        checkScore(score);
        synchronized (mutex) {
            AvProxy last = proxies.getLast();
            while (last != null && last.getAvgScore() < score) {
                last.offline();
                last = proxies.getLast();
            }
        }
    }

    private void checkScore(double score) {
        if (score < 0 || score > 1) {
            throw new IllegalStateException("avgScore for a AvProxy need between 0 and 1");
        }
    }

    public Iterator<? extends AvProxy> values() {
        return proxies.iterator();// 保证顺序
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
    public  int allSize(){
        return proxies.size() + blockedProxies.size();
    }

    public AvProxy hint(int hash) {
        if (consistentBuckets.size() == 0) {
            return null;
        }
        SortedMap<Integer, AvProxy> tmap = this.consistentBuckets.tailMap(hash);
        return (tmap.isEmpty()) ? consistentBuckets.firstEntry().getValue() : tmap.get(tmap.firstKey());
    }

    // 数据结构需要改造这个东西。。。我们需要一个高度灵活的优先级队列。但是不保证公平的优先级和绝对优先级。
    // ConcurrentLinkedQueue只是一个队列的时候。不能做到在队列中间插入数据z
    // private ConcurrentLinkedQueue<AvProxy> avProxies = new ConcurrentLinkedQueue<>();

    // monitor

    public double getRatio() {
        return ratio;
    }
}
