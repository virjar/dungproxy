package com.virjar.dungproxy.client.model;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.virjar.dungproxy.client.ippool.DomainPool;
import com.virjar.dungproxy.client.ippool.IpPool;
import com.virjar.dungproxy.client.ippool.config.Context;
import com.virjar.dungproxy.client.ippool.strategy.Scoring;

/**
 * Description: AvProxy
 *
 * @author lingtong.fu
 * @version 2016-09-11 09:52
 */
public class AvProxy {

    private static AtomicLong proxyNumberChange = new AtomicLong(0);

    // IP地址
    private String ip;

    // 端口号
    private Integer port;

    private boolean isInit = true;

    // 最后被引用的时间
    private long lastUsedTime = 0;

    private DomainPool domainPool;

    private boolean disable = false;

    // 引用次数,当引用次数为0的时候,由调度任务清除该
    private AtomicInteger referCount = new AtomicInteger(0);

    private AtomicInteger failedCount = new AtomicInteger(0);

    // 平均打分 需要归一化,在0-1的一个小数
    private double avgScore = 0;

    private AtomicInteger sucessTimes = new AtomicInteger(0);

    public AvProxy() {
        recordProxyChange();
    }

    public static void recordProxyChange() {
        if (proxyNumberChange.incrementAndGet() % 10 == 0) {// 序列化
            if(IpPool.getInstance() == null){
                return;//说明是初始化的时候,在递归调用到这里了。放弃序列化
            }
            Context.getInstance().getAvProxyDumper().serializeProxy(Maps.transformValues(
                    IpPool.getInstance().getPoolInfo(), new Function<List<AvProxy>, List<AvProxyVO>>() {
                        @Override
                        public List<AvProxyVO> apply(List<AvProxy> input) {
                            return Lists.transform(input, new Function<AvProxy, AvProxyVO>() {
                                @Override
                                public AvProxyVO apply(AvProxy input) {
                                    return AvProxyVO.fromModel(input);
                                }
                            });
                        }
                    }));
        }
    }

    // 虽然加锁,但是锁等待概率很小
    public synchronized void reset() {
        referCount.set(0);
        failedCount.set(0);
        disable = false;
    }

    public void recordFailed() {
        Scoring scoring = Context.getInstance().getScoring();
        if (sucessTimes.get() > 1) {
            while (sucessTimes.getAndDecrement() > 1) {
                avgScore = scoring.newAvgScore(this, Context.getInstance().getScoreFactory(), true);
                // score.setAvgScore(scoring.newAvgScore(score, Context.getInstance().getScoreFactory(), true));
            }
        }
        sucessTimes.set(0);
        avgScore = scoring.newAvgScore(this, Context.getInstance().getScoreFactory(), false);
        failedCount.incrementAndGet();
        if (Context.getInstance().getOffliner().needOffline(this)) {
            offline();// 资源下线,下次将不会分配这个IP了
        } else {
            domainPool.adjustPriority(this);
        }
    }

    public void adjustPriority() {
        domainPool.adjustPriority(this);
    }

    public void recordUsage() {
        sucessTimes.incrementAndGet();
        if (sucessTimes.get() > 1) {
            while (sucessTimes.getAndDecrement() > 1) {
                Scoring scoring = Context.getInstance().getScoring();
                avgScore = scoring.newAvgScore(this, Context.getInstance().getScoreFactory(), true);
                // score.setAvgScore(scoring.newAvgScore(score, Context.getInstance().getScoreFactory(), true));
            }
        }
        lastUsedTime = System.currentTimeMillis();
        referCount.incrementAndGet();
    }

    public void offline() {
        recordProxyChange();
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

    @Deprecated
    public AvProxy copy() {
        AvProxy newProxy = new AvProxy();
        newProxy.domainPool = domainPool;
        newProxy.disable = disable;
        newProxy.ip = ip;
        newProxy.isInit = isInit;
        newProxy.port = port;
        newProxy.failedCount = new AtomicInteger(failedCount.get());
        newProxy.referCount = new AtomicInteger(referCount.get());
        newProxy.avgScore = avgScore;
        // newProxy.score = new Score(score.getAvgScore(), new AtomicInteger(score.getFailedCount().get()),
        // new AtomicInteger(score.getReferCount().get()));
        return newProxy;
    }

    public Integer getFailedCount() {
        return failedCount.get();
    }

    public Integer getReferCount() {
        return referCount.get();
    }

    public double getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(double avgScore) {
        this.avgScore = avgScore;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = new AtomicInteger(failedCount);
    }

    public void setReferCount(Integer referCount) {
        this.referCount = new AtomicInteger(referCount);
    }
}
