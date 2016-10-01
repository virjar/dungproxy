package com.virjar.model;

import com.virjar.ipproxy.ippool.DomainPool;

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

    // 引用次数,当引用次数为0的时候,由调度任务清除该
    private int referCount = 0;

    private boolean isInit = true;

    // 平均打分
    private long avgScore = 0;

    private DomainPool domainPool;

    public void offline() {
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

    public long getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(long avgScore) {
        this.avgScore = avgScore;
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

    public int getReferCount() {
        return referCount;
    }

    public void setReferCount(int referCount) {
        this.referCount = referCount;
    }

    public DomainPool getDomainPool() {
        return domainPool;
    }

    public void setDomainPool(DomainPool domainPool) {
        this.domainPool = domainPool;
    }
}
