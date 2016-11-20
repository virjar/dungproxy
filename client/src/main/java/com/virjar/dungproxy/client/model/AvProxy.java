package com.virjar.dungproxy.client.model;

import java.util.concurrent.atomic.AtomicInteger;

import com.alibaba.fastjson.JSONObject;
import com.virjar.dungproxy.client.ippool.DomainPool;
import com.virjar.dungproxy.client.ippool.config.Context;
import com.virjar.dungproxy.client.ippool.strategy.Scoring;

/**
 * Description: AvProxy
 *
 * @author lingtong.fu
 * @version 2016-09-11 09:52
 */
public class AvProxy {

    // IP地址
    private String ip;

    // 端口号
    private Integer port;

    private boolean isInit = true;

    // 最后被引用的时间
    private long lastUsedTime = 0;

    private DomainPool domainPool;

    private boolean disable = false;

    // 打分保证
    private Score score = new Score();

    private AtomicInteger sucessTimes = new AtomicInteger(0);

    // 虽然加锁,但是锁等待概率很小
    public synchronized void reset() {
        score.getReferCount().set(0);
        score.getFailedCount().set(0);
        disable = false;
    }

    public void recordFailed() {
        Scoring scoring = Context.getInstance().getScoring();
        while (sucessTimes.getAndDecrement() > 0) {// 这个基本不会发生
            score.setAvgScore(scoring.newAvgScore(score, Context.getInstance().getScoreFactory(), true));
        }
        score.setAvgScore(scoring.newAvgScore(score, Context.getInstance().getScoreFactory(), false));
        score.getFailedCount().incrementAndGet();
        if (Context.getInstance().getOffliner().needOffline(score)) {
            offline();// 资源下线,下次将不会分配这个IP了
        } else {
                if(domainPool == null){
                    JSONObject.toJSONString(this);
                }
            domainPool.adjustPriority(this);
        }
    }

    public void adjustPriority() {
        domainPool.adjustPriority(this);
    }

    public void recordUsage() {
        while (sucessTimes.getAndDecrement() > 0) {
            Scoring scoring = Context.getInstance().getScoring();
            score.setAvgScore(scoring.newAvgScore(score, Context.getInstance().getScoreFactory(), true));
        }
        sucessTimes.incrementAndGet();
        lastUsedTime = System.currentTimeMillis();
        score.getReferCount().incrementAndGet();
    }

    public void offline() {
        disable = true;
        domainPool.offline(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AvProxy avProxy = (AvProxy) o;

        return ip != null ? ip.equals(avProxy.ip)
                : avProxy.ip == null && (port != null ? port.equals(avProxy.port) : avProxy.port == null);

    }

    @Override
    public int hashCode() {
        int result = ip != null ? ip.hashCode() : 0;
        result = 31 * result + (port != null ? port.hashCode() : 0);
        return result;
    }

    public boolean isDisable() {
        return disable;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isInit() {
        return isInit;
    }

    public void setInit(boolean init) {
        isInit = init;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public DomainPool getDomainPool() {
        return domainPool;
    }

    public void setDomainPool(DomainPool domainPool) {
        this.domainPool = domainPool;
    }

    public AvProxy copy() {
        AvProxy newProxy = new AvProxy();
        newProxy.domainPool = domainPool;
        newProxy.disable = disable;
        newProxy.ip = ip;
        newProxy.isInit = isInit;
        newProxy.port = port;
        newProxy.score = score;
        return newProxy;
    }

    public Score getScore() {
        return score;
    }

    public long getFailedCount() {
        return score.getFailedCount().get();
    }

    public long getReferCount() {
        return score.getReferCount().get();
    }
}
