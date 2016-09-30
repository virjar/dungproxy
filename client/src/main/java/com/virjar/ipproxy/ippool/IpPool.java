package com.virjar.ipproxy.ippool;

import java.util.Map;

import com.google.common.collect.Maps;
import com.virjar.model.AvProxy;

/**
 * Description: IpListPool
 *
 * @author lingtong.fu
 * @version 2016-09-03 01:13
 */
public class IpPool {

    private Map<String, DomainPool> pool = Maps.newConcurrentMap();

    private IpPool() {
    }

    private static IpPool instance = new IpPool();

    public static IpPool getInstance() {
        return instance;
    }

    public AvProxy bind(String host, String url, Object userID) {
        if (!pool.containsKey(host)) {
            synchronized (this) {// TODO import 参数何时放到系统,暂时放置默认
                if (!pool.containsKey(host)) {
                    pool.put(host, new DomainPool(host, null));
                }
            }
        }
        return pool.get(host).bind(url, userID);
    }

}
