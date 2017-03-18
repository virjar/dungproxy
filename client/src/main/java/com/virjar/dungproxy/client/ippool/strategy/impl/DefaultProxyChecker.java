package com.virjar.dungproxy.client.ippool.strategy.impl;

import com.virjar.dungproxy.client.ippool.strategy.ProxyChecker;
import com.virjar.dungproxy.client.model.AvProxyVO;
import com.virjar.dungproxy.client.util.IpAvValidator;

/**
 * Created by virjar on 17/2/25.<br/>
 * 默认使用httpclient,最终返回会HTTP_OK视为检查成功
 */
public class DefaultProxyChecker implements ProxyChecker {
    @Override
    public boolean available(AvProxyVO avProxyVO, String url) {
        return IpAvValidator.available(avProxyVO, url);
    }
}
