package com.virjar.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class DomainIpModel {
    private Long id;

    private String domain;

    private Long proxyId;

    private String ip;

    private Integer port;

    private Long domainScore;

    private Date domainScoreDate;

    private Date createtime;

    private Long speed;

    private String testUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public Long getProxyId() {
        return proxyId;
    }

    public void setProxyId(Long proxyId) {
        this.proxyId = proxyId;
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

    public Long getDomainScore() {
        return domainScore;
    }

    public void setDomainScore(Long domainScore) {
        this.domainScore = domainScore;
    }

    public Date getDomainScoreDate() {
        return domainScoreDate;
    }

    public void setDomainScoreDate(Date domainScoreDate) {
        this.domainScoreDate = domainScoreDate;
    }

    public Date getCreatetime() {
        return createtime;
    }

    public void setCreatetime(Date createtime) {
        this.createtime = createtime;
    }

    public Long getSpeed() {
        return speed;
    }

    public void setSpeed(Long speed) {
        this.speed = speed;
    }

    public String getTestUrl() {
        return testUrl;
    }

    public void setTestUrl(String testUrl) {
        this.testUrl = testUrl;
    }
}