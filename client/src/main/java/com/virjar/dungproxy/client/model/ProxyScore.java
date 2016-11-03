package com.virjar.dungproxy.client.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by virjar on 16/11/2.<br/>
 * 存储和计算分值的容器,一个proxy对象应该持有一个
 */
public class ProxyScore {
    private AtomicLong usedCount;
    private AtomicLong failedCount;
    //一个影响因子
    private int scoreFactory =10;
    private long avgScore;
}
