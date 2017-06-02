package com.virjar.dungproxy.webmagic7;

import com.virjar.dungproxy.client.ippool.IpPool;
import com.virjar.dungproxy.client.model.AvProxy;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.proxy.Proxy;
import us.codecraft.webmagic.proxy.ProxyProvider;

/**
 * Created by virjar on 17/6/2.
 * 
 * @since 0.0.1
 */
public class DungProxyProvider implements ProxyProvider {
    private String host;
    private String testUrl;
    private OfflineStrategy offlineStrategy;

    public DungProxyProvider(String host, OfflineStrategy offlineStrategy, String testUrl) {
        this.host = host;
        this.offlineStrategy = offlineStrategy;
        this.testUrl = testUrl;
    }

    public DungProxyProvider(String host, String testUrl) {
        this(host, new OfflineStrategy.NotOfflineStrategy(), testUrl);
    }

    private IpPool ipPool = IpPool.getInstance();

    @Override
    public void returnProxy(Proxy proxy, Page page, Task task) {
        if (!(proxy instanceof WebMagicBridgeProxy)) {
            return;
        }
        WebMagicBridgeProxy webMagicBridgeProxy = (WebMagicBridgeProxy) proxy;
        if (offlineStrategy.needOfflineProxy(page)) {
            webMagicBridgeProxy.getAvProxy().offline();
        }

    }

    @Override
    public Proxy getProxy(Task task) {
        AvProxy bind = ipPool.bind(host, testUrl);
        if (bind == null) {
            return null;
        }
        return new WebMagicBridgeProxy(bind);
    }
}
