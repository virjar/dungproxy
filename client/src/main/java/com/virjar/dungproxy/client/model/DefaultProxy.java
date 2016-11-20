package com.virjar.dungproxy.client.model;

import org.apache.http.HttpHost;

import com.virjar.dungproxy.client.util.IpAvValidator;

/**
 * Created by virjar on 16/10/29.
 */
public class DefaultProxy extends AvProxy {
    /**
     * 统一代理服务不走普通下线策略逻辑,因为她的失败不能表示统一代理服务器不可用。而可能是统一代理服务的上游挂了
     * 另外,统一代理服务目前没有自动上线逻辑
     */
    @Override
    public void offline() {//TODO 统一代理服务的上线逻辑
        if (!IpAvValidator.validateProxyConnect(new HttpHost(this.getIp(), getPort()))) {
            //super.offline();
        }
    }
}
