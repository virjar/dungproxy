package com.virjar.dungproxy.client.ippool.strategy.impl;

import com.virjar.dungproxy.client.ippool.strategy.ProxyChecker;
import com.virjar.dungproxy.client.model.AvProxyVO;
import com.virjar.dungproxy.client.util.IpAvValidator;

/**
 * Created by virjar on 17/2/25.<br/>
 * 检查IP是否可用,由于实际情况IP可用标准多异,所以这里抽象为接口,默认使用httpGet,响应码为HTTP_OK,
 * 如果你的网站有特殊可用性表现,则应该使用自己的检查器(如post,如多请求流程判断)。例如滑块验证码的打码代理,则需要特殊的检查器
 */
public class DefaultProxyChecker implements ProxyChecker {
    @Override
    public boolean available(AvProxyVO avProxyVO, String url) {
        return IpAvValidator.available(avProxyVO, url);
    }
}
