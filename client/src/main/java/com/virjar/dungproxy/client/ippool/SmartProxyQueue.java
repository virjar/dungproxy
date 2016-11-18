package com.virjar.dungproxy.client.ippool;

import java.util.concurrent.ConcurrentLinkedQueue;

import com.virjar.dungproxy.client.model.AvProxy;

/**
 * 智能的代理管理容器。他是单独为代理IP这种打分模型设计的容器<br/>
 * Created by virjar on 16/11/15.
 */
public class SmartProxyQueue {
    private double ratio;

    public SmartProxyQueue() {
        this(0.3);
    }

    public SmartProxyQueue(double ratio) {
        if (ratio < 0 || ratio > 1) {
            throw new IllegalStateException("ratio for SmartProxyQueue need between 0 and 1");
        }
        this.ratio = ratio;
    }

    // 数据结构需要改造这个东西。。。我们需要一个高度灵活的优先级队列。但是不保证公平的优先级和绝对优先级。
    // ConcurrentLinkedQueue只是一个队列的时候。不能做到在队列中间插入数据z
    private ConcurrentLinkedQueue<AvProxy> avProxies = new ConcurrentLinkedQueue<>();

}
