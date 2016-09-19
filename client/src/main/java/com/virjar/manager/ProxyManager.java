package com.virjar.manager;

import com.virjar.common.util.LogUtils;
import com.virjar.ipproxy.httpclient.ippool.IpPool;
import com.virjar.ipproxy.httpclient.ippool.IpPoolConfig;
import com.virjar.ipproxy.httpclient.ippool.IpPooledObjectFactory;
import com.virjar.model.AvProxy;

/**
 * Description: ProxyManager
 *
 * @author lingtong.fu
 * @version 2016-09-06 19:48
 */
public class ProxyManager {

    private IpPool ipPool;

    public ProxyManager() {
        LogUtils.info("ProxyManager ------ init");
        initIpPool();
    }

    private void initIpPool() {
        LogUtils.info("ProxyManager ------ initIpPool");
        IpPoolConfig ipPoolConfig = new IpPoolConfig();
        IpPooledObjectFactory ipPooledObjectFactory = new IpPooledObjectFactory();
        this.ipPool = new IpPool(ipPooledObjectFactory, ipPoolConfig);
    }

    public AvProxy getAvProxy() throws Exception {
        return ipPool.borrowObject();
    }
}
