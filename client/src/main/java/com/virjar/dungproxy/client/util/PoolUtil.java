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

    /**
     * 下线绑定在当前httpClient里面的IP
     * @param httpClientContext httpClientContext
     */
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

    /**
     * 清除IP和当前httpClientContext的绑定关系,这个不会导致IP下线
     */
    public static void cleanProxy(HttpClientContext httpClientContext) {
        httpClientContext.removeAttribute(ProxyConstant.USED_PROXY_KEY);
    }

    /**
     * 将任意一个代表user的对象绑定到http上下文,只要经过此步骤,执行过这个步骤之后,这个用户的cookie空间隔离(当然需要httpclient本身构建的时候的组建注册配合),
     * crawlerHttpClient本身则是支持这个功能的
     *
     * @param httpClientContext http的上下文
     * @param userId 代表用户信息的对象
     */
    public static void bindUserKey(HttpClientContext httpClientContext, String userId) {
        // throw new UnsupportedOperationException("废弃一致性hash绑定的支持,用户如果需要实现,自己上层维护,IP池IP变化太快,不容易维护");
        httpClientContext.setAttribute(ProxyConstant.DUNGPROXY_USER_KEY, userId);
    }

    /**
     * 获取绑定在当前httpClientContext里面的IP
     * @param httpClientContext
     * @return IP实例
     */
    public static AvProxy getBindProxy(HttpClientContext httpClientContext) {
        return httpClientContext.getAttribute(ProxyConstant.USED_PROXY_KEY, AvProxy.class);
    }

    /**
     * 禁止 com.virjar.dungproxy.client.httpclient.conn.ProxyBindRoutPlanner 插件使用dungproxy
     * 
     * @see com.virjar.dungproxy.client.httpclient.conn.ProxyBindRoutPlanner
     */
    public static void disableDungProxy(HttpClientContext httpClientContext) {
        httpClientContext.setAttribute(ProxyConstant.DISABLE_DUNGPROXY_KEY, Boolean.TRUE);
    }

    /**
     * dungProxy是否启用
     * 
     * @param httpClientContext
     * @return
     */
    public static boolean isDungProxyEnabled(HttpClientContext httpClientContext) {
        Object attribute = httpClientContext.getAttribute(ProxyConstant.DISABLE_DUNGPROXY_KEY);
        return attribute == null || !Boolean.TRUE.equals(attribute);
    }

}
