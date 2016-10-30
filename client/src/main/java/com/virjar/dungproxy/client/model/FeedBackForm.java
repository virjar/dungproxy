package com.virjar.dungproxy.client.model;

import java.util.List;

/**
 * Created by virjar on 16/10/3.
 */
public class FeedBackForm {
    private String domain;

    private List<AvProxy> avProxy;
    private List<AvProxy> disableProxy;

    public List<AvProxy> getAvProxy() {
        return avProxy;
    }

    public void setAvProxy(List<AvProxy> avProxy) {
        this.avProxy = avProxy;
    }

    public List<AvProxy> getDisableProxy() {
        return disableProxy;
    }

    public void setDisableProxy(List<AvProxy> disableProxy) {
        this.disableProxy = disableProxy;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }
}
