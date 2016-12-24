package com.virjar.dungproxy.client.ippool.strategy;

import com.virjar.dungproxy.client.model.AvProxy;

/**
 * Created by virjar on 16/10/1.
 */
public interface Offline {
    /**
     * 是否需要下线一个资源
     * 
     * @param avProxy 代理实例
     * @return 是否需要下线
     */
    boolean needOffline(AvProxy avProxy);
}
