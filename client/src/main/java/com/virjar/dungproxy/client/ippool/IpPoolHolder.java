package com.virjar.dungproxy.client.ippool;

import com.virjar.dungproxy.client.ippool.config.DungProxyContext;

/**
 * Created by virjar on 17/1/23. <br/>
 * 为了兼容老的设计方式,提供的一个静态方法
 */
public class IpPoolHolder {
    private static IpPool ipPool = null;

    public static IpPool getIpPool() {
        if (ipPool == null) {
            synchronized (IpPoolHolder.class) {
                if (ipPool == null) {
                    ipPool = IpPool.create(DungProxyContext.create().buildDefaultConfigFile().handleConfig());
                }
            }
        }
        return ipPool;
    }
}
