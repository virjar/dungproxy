package com.virjar.dungproxy.client.util;

import org.apache.http.client.protocol.HttpClientContext;

import com.virjar.dungproxy.client.ippool.config.ProxyConstant;
import com.virjar.dungproxy.client.model.AvProxy;

/**
 * Created by virjar on 16/10/4.
 */
public class PoolUtil {
    /**
     * 记录代理IP使用失败,提供这个接口的原因是代理IP访问和普通访问相比有很多不可预知性,<br/>
     * 目前IP池会根据http请求的异常来记录代理访问失败,但是很多请求看起来是正常的,但是实际上内容也不是我们预期的<br/>
     * 比如http://pachong.org/anonymous.html,有些代理能够拿到数据,有些却会跳转到一个域名备案页面<br/>
     * 通过这个机制让上层能够根据业务逻辑做失败反馈
     * 
     * @param httpClientContext http的上下文环境
     */
    public static void recordFailed(HttpClientContext httpClientContext) {
        if (httpClientContext == null) {
            return;
        }
        AvProxy attribute = httpClientContext.getAttribute(ProxyConstant.USED_PROXY_KEY, AvProxy.class);
        if (attribute != null) {
            attribute.recordFailed();
        }
    }

    public static void offline(HttpClientContext httpClientContext) {
        if (httpClientContext == null) {
            return;
        }
        AvProxy attribute = httpClientContext.getAttribute(ProxyConstant.USED_PROXY_KEY, AvProxy.class);
        if (attribute != null) {
            httpClientContext.removeAttribute(ProxyConstant.USED_PROXY_KEY);
            attribute.recordFailed();
            attribute.offline();

        }
    }

    public static void cleanProxy(HttpClientContext httpClientContext) {
        httpClientContext.removeAttribute(ProxyConstant.USED_PROXY_KEY);
    }

    /**
     * 将任意一个代表user的对象绑定到http上下文,只要经过此步骤,对应user基本每次都会被绑定到同一个IP上面。<br/>
     * 适用场景,多个僵尸账户登录目标网站爬取各自所见数据。要求各个用户cookie空间独立, 要求各个账户每次IP保持相同<br/>
     * 注意,IP池根据用户ID的hash值做一致性哈希绑定,请注意userID对象的hashCode函数是否会被均匀散列
     *
     * @deprecated  本功能过度设计,IP和用户的绑定对于大多数抓去场景来说是不必要的,所以废弃这个功能
     * @param httpClientContext http的上下文
     * @param userId 代表用户信息的对象
     */
    @Deprecated
    public static void bindUserKey(HttpClientContext httpClientContext, Object userId) {
       // httpClientContext.setAttribute(ProxyConstant.USER_KEY, userId);
    }

    public static AvProxy getBindProxy(HttpClientContext httpClientContext) {
        return httpClientContext.getAttribute(ProxyConstant.USED_PROXY_KEY, AvProxy.class);
    }
}
