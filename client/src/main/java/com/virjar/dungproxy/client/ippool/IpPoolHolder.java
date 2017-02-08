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
                    ipPool = IpPool.create(DungProxyContext.create().buildDefaultConfigFile());
                }
            }
        }
        return ipPool;
    }

    public static IpPool init(DungProxyContext dungProxyContext) {
        if (ipPool != null) {
            throw new IllegalStateException("默认IP池实例不允许多次实例化,您是在调用这个方法之前执行了默认实例化动作吗?");
        }
        synchronized (IpPoolHolder.class) {
            if (ipPool != null) {
                throw new IllegalStateException("默认IP池实例不允许多次实例化,您是在调用这个方法之前执行了默认实例化动作吗?");
            }
            ipPool = IpPool.create(dungProxyContext);
        }
        return ipPool;
    }
}
