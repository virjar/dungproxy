package com.virjar.dungproxy.client.ippool;

import java.util.*;

import com.google.common.collect.Lists;
import com.virjar.dungproxy.client.model.AvProxy;

/**
 * 智能的代理管理容器。他是单独为代理IP这种打分模型设计的容器<br/>
 * Created by virjar on 16/11/15.
 */
public class SmartProxyQueue {
    private double ratio;

    private final Object mutex = this;

    // 效率不高,但是先用这个实现.我们使用两个数据结构来实现绑定IP和优先级IP两种方案
    private LinkedList<AvProxy> proxies = Lists.newLinkedList();
    private TreeMap<Integer, AvProxy> consistentBuckets = new TreeMap<>();

    public SmartProxyQueue() {
        this(0.3);
    }

    public SmartProxyQueue(double ratio) {
        if (ratio < 0 || ratio > 1) {
            throw new IllegalStateException("ratio for SmartProxyQueue need between 0 and 1");
        }
        this.ratio = ratio;
    }

    /**
     * 一般用在初始化的时候,
     *
     * @param avProxy
     */
    public void addProxy(AvProxy avProxy) {
        synchronized (mutex) {
            proxies.addFirst(avProxy);
            consistentBuckets.put(avProxy.hashCode(), avProxy);
        }
    }

    /**
     * 一般用在初始化的时候,向容器中增加代理
     *
     * @param avProxies
     */
    public void addAllProxy(Collection<AvProxy> avProxies) {
        synchronized (mutex) {
            proxies.addAll(avProxies);
            for (AvProxy avProxy : avProxies) {
                consistentBuckets.put(avProxy.hashCode(), avProxy);
            }
        }
    }

    public void addWithScore(AvProxy avProxy) {
        checkScore(avProxy.getScore().getAvgScore());
        synchronized (mutex) {
            int index = (int) (proxies.size() * (ratio + (1 - ratio) * avProxy.getScore().getAvgScore()));
            proxies.add(index, avProxy);
            consistentBuckets.put(avProxy.hashCode(), avProxy);
        }

    }

    public AvProxy getAndAdjustPriority() {
        synchronized (mutex) {
            AvProxy poll = proxies.poll();
            if (poll == null) {
                return null;
            }
            int index = (int) (proxies.size() * ratio);
            proxies.add(index, poll);
            return poll;
        }
    }

    public void adjustPriority(AvProxy avProxy) {
        if (consistentBuckets.containsKey(avProxy.hashCode())) {// 如果已经下线,则不进行优先级调整动作
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
            while (last != null && last.getScore().getAvgScore() < score) {
                proxies.removeLast();
                consistentBuckets.remove(last.hashCode());
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
        return proxies.iterator();
    }

    public int size() {
        return proxies.size();
    }

    public AvProxy hint(int hash) {
        if (consistentBuckets.size() == 0) {
            return null;
        }
        SortedMap<Integer, AvProxy> tmap = this.consistentBuckets.tailMap(hash);
        return (tmap.isEmpty()) ? consistentBuckets.firstEntry().getValue() : tmap.get(tmap.firstKey());
    }

    public void remove(AvProxy avProxy) {
        synchronized (mutex) {
            proxies.remove(avProxy);
            consistentBuckets.remove(avProxy.hashCode());
        }
    }

    // 数据结构需要改造这个东西。。。我们需要一个高度灵活的优先级队列。但是不保证公平的优先级和绝对优先级。
    // ConcurrentLinkedQueue只是一个队列的时候。不能做到在队列中间插入数据z
    // private ConcurrentLinkedQueue<AvProxy> avProxies = new ConcurrentLinkedQueue<>();

}
