package com.virjar.dungproxy.webmagic7;

import com.virjar.dungproxy.client.model.AvProxy;

import us.codecraft.webmagic.proxy.Proxy;

/**
 * Created by virjar on 17/6/2.
 */
public class WebMagicBridgeProxy extends Proxy {
    private AvProxy avProxy;

    public WebMagicBridgeProxy(AvProxy avProxy) {
        super(avProxy.getIp(), avProxy.getPort(), avProxy.getUsername(), avProxy.getPassword());
        this.avProxy = avProxy;
    }

    public AvProxy getAvProxy() {
        return avProxy;
    }
}
