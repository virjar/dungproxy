package com.virjar.ipproxy.httpclient.conn;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.virjar.ipproxy.ippool.IpPool;
import com.virjar.ipproxy.ippool.config.Context;
import com.virjar.ipproxy.ippool.config.ProxyConstant;
import com.virjar.ipproxy.ippool.strategy.proxydomain.ProxyDomainStrategy;
import com.virjar.model.AvProxy;
import com.virjar.model.UserEnv;

/**
 * 如果是和原生httpclient集成,必须将她放置到httpclientBuilder中,如果使用我们默认提供的,则无需设置,他将会自动植入<br/>
 * 通过这个绑定代理IP,这种方式可以以非侵入的形式和原声httpclient集成,<br/>
 * 甚至只要在原声httpclientBuilder里面植入本类,即可实现代理自动接入服务<br/>
 * Created by virjar on 16/9/28.
 */
public class ProxyBindRoutPlanner extends DefaultRoutePlanner {

    public ProxyBindRoutPlanner() {
        super(null);
    }

    private static final Logger logger = LoggerFactory.getLogger(ProxyBindRoutPlanner.class);

    /**
     * @param schemePortResolver schema解析器,可以传空,这个时候将会使用默认
     *            {org.apache.http.impl.conn.DefaultSchemePortResolver#INSTANCE}
     */
    public ProxyBindRoutPlanner(SchemePortResolver schemePortResolver) {
        super(schemePortResolver);
    }

    @Override
    protected HttpHost determineProxy(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        ProxyDomainStrategy needProxyStrategy = Context.getInstance().getNeedProxyStrategy();
        if (!needProxyStrategy.needProxy(target.getHostName())) {
            return null;
        }

        String accessUrl = null;
        // TODO 有问题,貌似post也会是这个,但是服务器现在只进行get验证,通过这种方式过滤选取了HttpRequestWrapper 和 HttpGet两种对象需要有一种方式过滤Get方法出来
        if (request instanceof HttpRequestWrapper || request instanceof HttpGet) {
            accessUrl = HttpUriRequest.class.cast(request).getURI().toString();
        }
        Object user = context.getAttribute(ProxyConstant.USER_KEY);
        AvProxy bind;
        // bind = (AvProxy) context.getAttribute(ProxyConstant.USED_PROXY_KEY);
        // if (bind == null) {
        bind = IpPool.getInstance().bind(target.getHostName(), accessUrl, user);
        // }
        if (bind != null) {
            bind.recordUsage();
            if (user != null) {// 记录这个用户绑定的IP
                UserEnv userEnv = UserEnv.class.cast(context.getAttribute(ProxyConstant.USER_ENV_CONTAINER_KEY));
                if (userEnv == null) {// 第一次访问,IP分配到用户
                    userEnv = new UserEnv();
                    context.setAttribute(ProxyConstant.USER_ENV_CONTAINER_KEY, userEnv);
                    userEnv.setUser(user);
                }
                if (!bind.equals(userEnv.getBindProxy())) {
                    // TODO IP 改变事件
                    userEnv.setBindProxy(bind);
                }
            }
            // 将绑定IP放置到context,用于后置拦截器统计这个IP的使用情况
            context.setAttribute(ProxyConstant.USED_PROXY_KEY, bind);
            return new HttpHost(bind.getIp(), bind.getPort());
        }

        return super.determineProxy(target, request, context);
    }
}
