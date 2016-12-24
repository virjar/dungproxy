package com.virjar.dungproxy.client.model;

import java.util.List;

/**
 * Created by virjar on 16/10/3.
 */
public class FeedBackForm {
    private String domain;

    private List<AvProxyVO> avProxy;
    private List<AvProxyVO> disableProxy;

    public List<AvProxyVO> getAvProxy() {
        return avProxy;
    }

    public void setAvProxy(List<AvProxyVO> avProxy) {
        this.avProxy = avProxy;
    }

    public List<AvProxyVO> getDisableProxy() {
        return disableProxy;
    }

    public void setDisableProxy(List<AvProxyVO> disableProxy) {
        this.disableProxy = disableProxy;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
