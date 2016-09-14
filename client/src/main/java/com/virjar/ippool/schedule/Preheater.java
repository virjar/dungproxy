package com.virjar.ippool.schedule;

import com.google.common.collect.Queues;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import com.virjar.client.http.HttpOption;
import com.virjar.client.proxyclient.VirjarAsyncClient;
import com.virjar.common.cache.Domains;
import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.entity.Proxy;
import com.virjar.ippool.IpPool;
import com.virjar.ippool.IpPoolConfig;
import com.virjar.ippool.IpPooledObjectFactory;
import com.virjar.model.AvProxy;
import com.virjar.repository.ProxyRepository;
import com.virjar.utils.JSONUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Description: 初始化时加载Proxy
 *
 *
 * @author lingtong.fu
 * @version 2016-09-11 18:16
 */
public class Preheater {

    private static VirjarAsyncClient client = new VirjarAsyncClient();

    private static final String url = "http://115.159.40.202:8080/proxyipcenter/av";

    private static volatile ConcurrentLinkedQueue<AvProxy> PROXY_QUEUE = Queues.newConcurrentLinkedQueue();

    private IpPool ipPool;

    @Resource
    private BeanMapper beanMapper;

    @Resource
    private ProxyRepository proxyRepository;

    public void init() {
        initProxyQueue();
        initIpPool();
    }

    private void initProxyQueue() {
        try {
            List<AvProxy> avProxies = JSONUtils.parseList(getFuture(url).get(60000, TimeUnit.MILLISECONDS), AvProxy.class);
            ConcurrentLinkedQueue<AvProxy> queue = Queues.newConcurrentLinkedQueue();
            assert avProxies != null;
            for (AvProxy avProxy : avProxies) {
                queue.add(avProxy);
            }
            PROXY_QUEUE = queue;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initIpPool() {
        IpPoolConfig ipPoolConfig = new IpPoolConfig();
        IpPooledObjectFactory ipPooledObjectFactory = new IpPooledObjectFactory();
        this.ipPool = new IpPool(ipPooledObjectFactory, ipPoolConfig);
    }

    public AvProxy getAvProxy() {
        AvProxy av = PROXY_QUEUE.poll();
        if (av == null) {
            synchronized (Preheater.class) {
                av = PROXY_QUEUE.poll();
                if (av == null) {
                    //暂时拿一个最高分的Proxy
                    List<Proxy> proxy = proxyRepository.findAvailable();
                    av = beanMapper.map(proxy.get(0), AvProxy.class);
                    if (av != null) {
                        PROXY_QUEUE.add(av);
                    }
                }
            }
        }
        return av;
    }

    private static Future<String> getFuture(String url) throws IOException {
        HttpOption httpOption = new HttpOption();
        httpOption.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        return client.get(url, httpOption, new AsyncCompletionHandler<String>() {
            @Override
            public String onCompleted(Response response) throws Exception {
                if (response.getStatusCode() == 200) {
                    return response.getResponseBody();
                }
                return null;
            }
        });
    }
}
