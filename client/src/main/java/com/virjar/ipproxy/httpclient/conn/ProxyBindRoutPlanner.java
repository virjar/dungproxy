package com.virjar.ipproxy.httpclient.conn;

import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.SchemePortResolver;
import org.apache.http.impl.conn.DefaultRoutePlanner;
import org.apache.http.protocol.HttpContext;

import com.virjar.ipproxy.ippool.IpPool;
import com.virjar.model.AvProxy;

/**
 * 通过这个绑定代理IP,这种方式可以以非侵入的形式和原声httpclient集成,<br/>
 * 甚至只要在原声httpclientBuilder里面植入本类,即可实现代理自动接入服务<br/>
 * Created by virjar on 16/9/28.
 */
public class ProxyBindRoutPlanner extends DefaultRoutePlanner {

    public ProxyBindRoutPlanner() {
        super(null);
    }

    /**
     * @param schemePortResolver schema解析器,可以传空,这个时候将会使用默认
     *            {org.apache.http.impl.conn.DefaultSchemePortResolver#INSTANCE}
     */
    public ProxyBindRoutPlanner(SchemePortResolver schemePortResolver) {
        super(schemePortResolver);
    }

    @Override
    protected HttpHost determineProxy(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
        String accessUrl = null;
        if (request instanceof HttpGet) {// TODO 有问题,貌似post也会是这个,但是服务器现在只进行get验证
            accessUrl = HttpUriRequest.class.cast(request).getURI().toString();
        }
        AvProxy bind = IpPool.getInstance().bind(target.getHostName(), accessUrl, null);
        if (bind != null) {
            return new HttpHost(bind.getIp(), bind.getPort());
        }
        return super.determineProxy(target, request, context);
    }
}
