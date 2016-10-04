package com.virjar.ipproxy.util;

import org.apache.http.client.protocol.HttpClientContext;

import com.virjar.ipproxy.ippool.config.ProxyConstant;
import com.virjar.model.AvProxy;

/**
 * Created by virjar on 16/10/4.
 */
public class PoolUtil {
    public static void recordFailed(HttpClientContext httpClientContext) {
        if (httpClientContext == null) {
            return;
        }
        AvProxy attribute = httpClientContext.getAttribute(ProxyConstant.USED_PROXY_KEY, AvProxy.class);
        if (attribute != null) {
            attribute.recordFailed();
        }
    }
}
