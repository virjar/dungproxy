package com.virjar.dungproxy.server.crawler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.virjar.dungproxy.server.entity.Proxy;

/**
 * Created by virjar on 16/11/26.
 */
public abstract class NewCollector {
    private static final Logger logger = LoggerFactory.getLogger(NewCollector.class);
    /**
     * 每次收集的期望数量
     */
    protected int batchSize = 100;
    /**
     * 收集时间间隔,分钟为单位
     */
    private int duration = 120;

    private int cleanDuration = 24 * 60 * 60 * 1000;
    /**
     * 当前时间已经收集的资源数目
     */
    private int collectedNumber;

    private long lastActiveTimeStamp = 0;

    private long lastCleanTimeStamp = System.currentTimeMillis();

    protected String errorInfo;

    public abstract String lasUrl();

    public abstract List<Proxy> doCollect();

    public List<Proxy> newProxy() {
        long timeStamp = System.currentTimeMillis();
        if (timeStamp - lastCleanTimeStamp > cleanDuration) {
            this.collectedNumber = 0;
            lastCleanTimeStamp = timeStamp;
        }
        if (timeStamp - lastActiveTimeStamp < duration * 60 * 1000) {
            return Lists.newArrayList();
        }
        List<Proxy> ret;
        try {
            ret = doCollect();
        } catch (Exception e) {
            ret = Lists.newArrayList();
            logger.error("收集器异常:", e);
            errorInfo = e.toString();
        }
        collectedNumber += ret.size();
        lastActiveTimeStamp = timeStamp;
        return ret;
    }

    public int getCollectedNumber() {
        return collectedNumber;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public String getErrorInfo() {
        return errorInfo;
    }

    public void setErrorInfo(String errorInfo) {
        this.errorInfo = errorInfo;
    }
}
