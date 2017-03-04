package com.virjar.dungproxy.client.ippool.strategy;

import com.virjar.dungproxy.client.model.AvProxyVO;

/**
 * Created by virjar on 17/2/25.
 */
public interface ProxyChecker {
    boolean available(AvProxyVO avProxyVO, String url);
}
