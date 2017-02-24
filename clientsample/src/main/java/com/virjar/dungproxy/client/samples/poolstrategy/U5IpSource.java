package com.virjar.dungproxy.client.samples.poolstrategy;

import java.io.StringReader;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.protocol.HttpClientContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;
import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.client.ippool.strategy.ResourceFacade;
import com.virjar.dungproxy.client.model.AvProxyVO;
import com.virjar.dungproxy.client.util.CommonUtil;
import com.virjar.dungproxy.client.util.PoolUtil;

/**
 * Created by virjar on 17/2/14.<br/>
 * 无忧代理接入demo data5u.com
 */
public class U5IpSource implements ResourceFacade {
    private static final Logger logger = LoggerFactory.getLogger(U5IpSource.class);
    /**
     * 无忧代理每次只能返回几个IP,但是短时间可以多次获取IP
     */
    private ThreadPoolExecutor pool = new ThreadPoolExecutor(10, 10, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), new ThreadPoolExecutor.CallerRunsPolicy());
    private List<AvProxyVO> cache = Lists.newLinkedList();// 链表效率高
    private ReentrantLock lock = new ReentrantLock();

    // 无忧代理每秒不能超过5此,所以添加限流器,我们发现无忧代理的实现不是这个规则,没有超过5此也会被封,测试发现每秒可以请求2次
    private RateLimiter rateLimiter = RateLimiter.create(2);//
    private final Splitter ipAndPortSpitter = Splitter.on(":");

    private class DownLoadThread extends Thread {
        @Override
        public void run() {
            while (true) {
                if (rateLimiter.tryAcquire(1, 500, TimeUnit.MILLISECONDS)) {// 500毫秒等待令牌桶的释放,guava限流器基于令牌桶算法
                    HttpClientContext httpClientContext = HttpClientContext.create();
                    PoolUtil.disableDungProxy(httpClientContext);
                    String s = HttpInvoker.get(url, httpClientContext);
                    logger.info("IP请求结果:{}", s);

                    LineIterator iterator = new LineIterator(new StringReader(s));
                    while (iterator.hasNext()) {
                        String line = iterator.nextLine();
                        System.out.println(line);
                        if (CommonUtil.isPlainProxyItem(line)) {
                            addProxy(line);
                        }
                    }
                } else {
                    logger.info("请求过于频繁,放弃本次请求");
                    CommonUtil.sleep(500);
                }
            }
        }
    }

    public U5IpSource() {
        // for (int i = 0; i < 2; i++) {//发现一个线程就够了
        pool.execute(new DownLoadThread());
        // }
    }

    private void addProxy(String line) {
        List<String> strings = ipAndPortSpitter.splitToList(line);
        AvProxyVO avProxyVO = new AvProxyVO();
        avProxyVO.setIp(strings.get(0));
        avProxyVO.setPort(NumberUtils.toInt(strings.get(1)));
        lock.lock();
        try {
            cache.add(avProxyVO);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 无忧代理提取地址
     */
    private static String url = "http://api.ip.data5u.com/dynamic/get.html?order=c7a31d45acd98e81d87655aa5d57baf9&random=true";

    @Override
    public List<AvProxyVO> importProxy(String domain, String testUrl, Integer number) {
        List<AvProxyVO> ret = Lists.newArrayList();
        lock.lock();
        try {
            int i = 0;
            Iterator<AvProxyVO> iterator = cache.iterator();
            while (iterator.hasNext()) {
                if (i < number) {
                    i++;
                    ret.add(iterator.next());
                    iterator.remove();
                } else {
                    break;
                }
            }
        } finally {
            lock.unlock();
        }
        return ret;
    }

    @Override
    public void feedBack(String domain, List<AvProxyVO> avProxies, List<AvProxyVO> disableProxies) {

    }

    @Override
    public List<AvProxyVO> allAvailable() {
        // 不支持在预热的时候使用无忧代理,因为预热会浪费IP资源
        return Lists.newArrayList();
    }
}
