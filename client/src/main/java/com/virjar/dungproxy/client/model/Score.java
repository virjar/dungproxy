package com.virjar.dungproxy.client.model;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by virjar on 16/11/18.
 */
public class Score {
    // 引用次数,当引用次数为0的时候,由调度任务清除该
    private AtomicInteger referCount = new AtomicInteger(0);

    private AtomicInteger failedCount = new AtomicInteger(0);

    // 平均打分 需要归一化,在0-1的一个小数
    private double avgScore = 0;


    public double getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(double avgScore) {
        this.avgScore = avgScore;
    }

    public AtomicInteger getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(AtomicInteger failedCount) {
        this.failedCount = failedCount;
    }

    public AtomicInteger getReferCount() {
        return referCount;
    }

    public void setReferCount(AtomicInteger referCount) {
        this.referCount = referCount;
    }
}
