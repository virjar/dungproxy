package com.virjar.ipproxy.ippool.schedule;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.RateLimiter;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import com.virjar.client.http.HttpOption;
import com.virjar.client.proxyclient.VirjarAsyncClient;
import com.virjar.common.util.LogUtils;
import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.entity.Proxy;
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
 * Description: 初始化时加载Proxy 定时收集Proxy
 *
 * @author lingtong.fu
 * @version 2016-09-11 18:16
 */
public class Preheater {

    public void init() {
        LogUtils.info("Preheater ------ init");
        initProxyQueue();
    }

    private void initProxyQueue() {
        LogUtils.info("Preheater ------ initProxyQueue");
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

    public AvProxy getAvProxy() {
        LogUtils.info("Preheater ------ getAvProxy");
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

    public void updateProxy() {
        LogUtils.info("Preheater ------ updateProxy");
        RateLimiter rateLimiter = RateLimiter.create(rateLimit);
        try {
            rateLimiter.acquire();
            List<AvProxy> avProxies = JSONUtils.parseList(getFuture(url).get(60000, TimeUnit.MILLISECONDS), AvProxy.class);
            if (avProxies != null) {
                RET_QUEUE.addAll(avProxies);
                for (int i = 0; i < updateLimit; i++) {
                    AvProxy ret = RET_QUEUE.poll();
                    if (ret == null) {
                        break;
                    } else {
                        // TODO 本地可用Proxy验证
                        PROXY_QUEUE.add(ret);
                    }
                }

                LogUtils.info("此次更新共获取 [{}] 个代理", RET_QUEUE.size());
            } else {
                LogUtils.error("此次更新失败 请检查av接口");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final int rateLimit = 1000;

    private final int updateLimit = 100;

    private static final String url = "http://115.159.40.202:8080/proxyipcenter/av";

    private static volatile ConcurrentLinkedQueue<AvProxy> PROXY_QUEUE = Queues.newConcurrentLinkedQueue();

    private static volatile ConcurrentLinkedQueue<AvProxy> RET_QUEUE = Queues.newConcurrentLinkedQueue();

    private static VirjarAsyncClient client = new VirjarAsyncClient();

    @Resource
    private BeanMapper beanMapper;

    @Resource
    private ProxyRepository proxyRepository;

}
