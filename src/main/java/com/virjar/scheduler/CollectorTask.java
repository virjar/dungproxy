package com.virjar.scheduler;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.crawler.Collector;
import com.virjar.entity.Proxy;
import com.virjar.model.ProxyModel;
import com.virjar.repository.ProxyRepository;
import com.virjar.service.ProxyService;
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

    private ExecutorService pool = Executors.newFixedThreadPool(SysConfig.getInstance().getIpCrawlerThread());

    public static List<Collector> getCollectors() {
        return Collectors;
    }

    static List<Collector> Collectors = null;
    static {
        try {
            logger.info("start Collector");
            Collectors = Collector.buildfromSource("/handmapper.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long totalWaitTime = 10 * 60 * 1000;
        logger.info("CollectorTask start");
        while (true) {
            try {
                Collections.sort(Collectors, new Comparator<Collector>() {
                    @Override
                    public int compare(Collector o1, Collector o2) {// 失败次数越多，被调度的可能性越小。成功的次数越多，被调度的可能性越小。没有成功也没有失败的，被调度的可能性最大
                        return (o1.getFailedTimes() * 10 - o1.getSucessTimes() * 3)
                                - (o2.getFailedTimes() * 10 - o2.getSucessTimes() * 3);
                    }
                });
                List<Future<Object>> futures = Lists.newArrayList();
                for (Collector collector : Collectors) {
                    futures.add(pool.submit(new WebsiteCollect(collector)));
                }
                long start = System.currentTimeMillis();
                for (Future<Object> future : futures) {
                    try {
                        // 等待十分钟
                        future.get(totalWaitTime + start - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                // do nothing
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(this).start();
    }

    private class WebsiteCollect implements Callable<Object> {

        private Collector collector;

        public WebsiteCollect(Collector collector) {
            super();
            this.collector = collector;
        }

        @Override
        public Object call() throws Exception {
            List<Proxy> draftproxys = collector.newProxy(proxyRepository);
            ResourceFilter.filter(draftproxys);
            proxyService.save(beanMapper.mapAsList(draftproxys, ProxyModel.class));
            return this;
        }
    }
}
