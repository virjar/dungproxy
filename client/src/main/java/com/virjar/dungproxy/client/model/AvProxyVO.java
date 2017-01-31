package com.virjar.dungproxy.client.model;

import java.util.List;

import org.apache.http.NameValuePair;

import com.google.common.collect.Lists;
import com.virjar.dungproxy.client.ippool.DomainPool;
import com.virjar.dungproxy.client.ippool.config.DomainContext;

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

    // 是否是云代理,云代理将会参与使用竞争,但是不会下线,云代理本身后面存在一个IP池,做数据转发
    private Boolean cloud;

    // 用户名,如果本IP需要登录认证
    private String username;

    // 密码,如果本IP需要登录认证
    private String password;

    // 有些代理是通过请求头进行认证的
    private List<NameValuePair> authenticationHeaders = Lists.newArrayList();

    public Boolean getCloud() {
        return cloud;
    }

    public void setCloud(Boolean cloud) {
        this.cloud = cloud;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<NameValuePair> getAuthenticationHeaders() {
        return Lists.newArrayList(authenticationHeaders);
    }

    public void setAuthenticationHeaders(List<NameValuePair> authenticationHeaders) {
        this.authenticationHeaders = authenticationHeaders;
    }

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

    public AvProxy toModel(DomainContext domainContext) {
        AvProxy avProxy;
        if (cloud != null && cloud) {
            avProxy = new CloudProxy(domainContext);
        } else {
            avProxy = new AvProxy(domainContext);
        }
        avProxy.setIp(ip);
        avProxy.setPort(port);
        avProxy.setAvgScore(avgScore);
        avProxy.setReferCount(referCount);
        avProxy.setFailedCount(failedCount);
        avProxy.setUsername(username);
        avProxy.setPassword(password);
        avProxy.setAuthenticationHeaders(getAuthenticationHeaders());
        return avProxy;
    }

    public AvProxy toModel(DomainPool domainPool) {
        return toModel(domainPool.getDomainContext());
    }

    public static AvProxyVO fromModel(AvProxy avProxy) {
        AvProxyVO avProxyVO = new AvProxyVO();
        avProxyVO.setIp(avProxy.getIp());
        avProxyVO.setPort(avProxy.getPort());
        avProxyVO.setFailedCount(avProxy.getFailedCount());
        avProxyVO.setReferCount(avProxy.getReferCount());
        avProxyVO.setAvgScore(avProxy.getAvgScore());
        if (avProxy.getDomainPool() != null) {// 今后的domainPool永远不能为null
            avProxyVO.setDomain(avProxy.getDomainPool().getDomain());
        }
        avProxyVO.setCloud(avProxy instanceof CloudProxy);

        avProxyVO.setAuthenticationHeaders(avProxy.getAuthenticationHeaders());
        avProxyVO.setUsername(avProxy.getUsername());
        avProxyVO.setPassword(avProxy.getPassword());
        return avProxyVO;
    }
}
