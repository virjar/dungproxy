package com.virjar.dungproxy.client.model;

/**
 * Created by virjar on 16/12/24. <br/>
 * 仅仅为了序列化和反序列化
 */
public class AvProxyVO {
    // IP地址
    private String ip;

    // 端口号
    private Integer port;

    // 引用次数,当引用次数为0的时候,由调度任务清除该
    private Integer referCount = 0;

    private Integer failedCount = 0;

    // 平均打分 需要归一化,在0-1的一个小数
    private double avgScore = 0D;

    // 属于那一个网站的代理IP
    private String domain;

    public double getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(double avgScore) {
        this.avgScore = avgScore;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getReferCount() {
        return referCount;
    }

    public void setReferCount(Integer referCount) {
        this.referCount = referCount;
    }

    public AvProxy toModel() {
        AvProxy avProxy = new AvProxy();
        avProxy.setIp(ip);
        avProxy.setPort(port);
        avProxy.setAvgScore(avgScore);
        avProxy.setReferCount(referCount);
        avProxy.setFailedCount(failedCount);
        return avProxy;
    }

    public static AvProxyVO fromModel(AvProxy avProxy) {
        AvProxyVO avProxyVO = new AvProxyVO();
        avProxyVO.setIp(avProxy.getIp());
        avProxyVO.setPort(avProxy.getPort());
        avProxyVO.setFailedCount(avProxy.getFailedCount());
        avProxyVO.setFailedCount(avProxy.getReferCount());
        avProxyVO.setAvgScore(avProxy.getAvgScore());
        if (avProxy.getDomainPool() != null) {// 今后的domainPool永远不能为null
            avProxyVO.setDomain(avProxy.getDomainPool().getDomain());
        }
        return avProxyVO;
    }
}
