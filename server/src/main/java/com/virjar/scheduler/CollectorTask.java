package com.virjar.scheduler;

import java.util.*;
import java.util.concurrent.*;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.crawler.Collector;
import com.virjar.entity.Proxy;
import com.virjar.ipproxy.util.CommonUtil;
import com.virjar.model.ProxyModel;
import com.virjar.repository.ProxyRepository;
import com.virjar.service.ProxyService;
import com.virjar.utils.NameThreadFactory;
import com.virjar.utils.ResourceFilter;
import com.virjar.utils.SysConfig;

@Component
public class CollectorTask implements Runnable, InitializingBean {

    @Resource
    private ProxyService proxyService;

    @Resource
    private BeanMapper beanMapper;

    @Resource
    private ProxyRepository proxyRepository;

    private static final Logger logger = LoggerFactory.getLogger(CollectorTask.class);

    private boolean isRunning = false;

    // 一般来说线程池不会有空转的,我希望所有线程能够随时工作,线程池除了节省线程创建和销毁开销,同时起限流作用,如果任务提交太多,则使用主线程进行工作
    // 从而阻塞主线程任务产生逻辑
    private ExecutorService pool = null;

    public static List<Collector> getCollectors() {
        return collectors;
    }

    static List<Collector> collectors = null;
    static {
        try {
            logger.info("start Collector");
            collectors = Collector.buildfromSource("/handmapper.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Map<Collector, Long> sleepTimeStamp = Maps.newHashMap();

    @Override
    public void run() {
        long totalWaitTime = 100 * 60 * 1000;
        logger.info("CollectorTask start");
        while (isRunning) {
            try {
                // logger.info("begin proxy collect start");
                Collections.sort(collectors, new Comparator<Collector>() {
                    @Override
                    public int compare(Collector o1, Collector o2) {// 失败次数越多，被调度的可能性越小。成功的次数越多，被调度的可能性越小。没有成功也没有失败的，被调度的可能性最大
                        return (o1.getFailedTimes() * 10 - o1.getSucessTimes() * 3)
                                - (o2.getFailedTimes() * 10 - o2.getSucessTimes() * 3);
                    }
                });
                List<Future<Object>> futures = Lists.newArrayList();
                for (Collector collector : collectors) {
                    futures.add(pool.submit(new WebsiteCollect(collector)));
                }
                CommonUtil.waitAllFutures(futures);
            } catch (Exception e) {
                // do nothing
                logger.error("error when collect proxy", e);
            }
        }
    }

    private void init() {
        isRunning = SysConfig.getInstance().getIpCrawlerThread() > 0;
        if (!isRunning) {
            logger.info("collector task is not running");
            return;
        }
        pool = new ThreadPoolExecutor(SysConfig.getInstance().getIpCrawlerThread(),
                SysConfig.getInstance().getIpCrawlerThread(), 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new NameThreadFactory("collector"),
                new ThreadPoolExecutor.CallerRunsPolicy());
        Random random = new Random();
        for (Collector collector : collectors) {
            sleepTimeStamp.put(collector, System.currentTimeMillis() + random.nextInt() % 60000);
        }
        new Thread(this).start();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();

    }

    private class WebsiteCollect implements Callable<Object> {

        private Collector collector;

        public WebsiteCollect(Collector collector) {
            super();
            this.collector = collector;

        }

        @Override
        public Object call() throws Exception {
            Long aLong = sleepTimeStamp.get(collector);// 开机随机暂停,放弃任务错开爬虫集中访问导致网络拥堵
            if (aLong != null && System.currentTimeMillis() < aLong) {
                return this;
            }
            List<Proxy> draftproxys = collector.newProxy(proxyRepository);
            // logger.info("收集到的新资源:{}", JSON.toJSONString(draftproxys));
            ResourceFilter.filter(draftproxys);
            proxyService.save(beanMapper.mapAsList(draftproxys, ProxyModel.class));
            return this;
        }
    }
}
