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

    public DungProxyProvider(String host, String testUrl, OfflineStrategy offlineStrategy) {
        this.host = host;
        this.offlineStrategy = offlineStrategy;
        this.testUrl = testUrl;
    }

    public DungProxyProvider(String host, String testUrl) {
        this(host, testUrl, new OfflineStrategy.NotOfflineStrategy());
    }

    @Override
    public void returnProxy(Proxy proxy, Page page, Task task) {
        if (!(proxy instanceof WebMagicBridgeProxy)) {
            return;
        }
        WebMagicBridgeProxy webMagicBridgeProxy = (WebMagicBridgeProxy) proxy;
        if (offlineStrategy.needOfflineProxy(page, webMagicBridgeProxy.getAvProxy())) {
            webMagicBridgeProxy.getAvProxy().offline();
        } else if (!page.isDownloadSuccess()) {
            webMagicBridgeProxy.getAvProxy().recordFailed();
        }

    }

    @Override
    public Proxy getProxy(Task task) {
        AvProxy bind = IpPool.getInstance().bind(host, testUrl);
        if (bind == null) {
            return null;
        }
        bind.recordUsage();
        return new WebMagicBridgeProxy(bind);
    }
}
