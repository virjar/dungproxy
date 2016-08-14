package com.virjar.scheduler;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.crawler.Collector;
import com.virjar.entity.Proxy;
import com.virjar.model.ProxyModel;
import com.virjar.repository.ProxyRepository;
import com.virjar.service.ProxyService;
import com.virjar.utils.ResourceFilter;

public class CollectorTask implements Runnable {

    @Resource
    private ProxyService proxyService;

    @Resource
    private BeanMapper beanMapper;

    @Resource
    private ProxyRepository proxyRepository;
    private static final Logger logger = LoggerFactory.getLogger(CollectorTask.class);

    private ThreadPoolTaskExecutor executor;

    public ThreadPoolTaskExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ThreadPoolTaskExecutor executor) {
        this.executor = executor;
    }

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

        Collections.sort(Collectors, new Comparator<Collector>() {
            @Override
            public int compare(Collector o1, Collector o2) {// 失败次数越多，被调度的可能性越小。成功的次数越多，被调度的可能性越小。没有成功也没有失败的，被调度的可能性最大
                return (o1.getFailedTimes() * 10 - o1.getSucessTimes() * 3)
                        - (o2.getFailedTimes() * 10 - o2.getSucessTimes() * 3);
            }
        });
        for (Collector Collector : Collectors) {
            try {
                executor.execute(new WebsiteCollect(Collector));
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("", e);
            }
        }
    }

    private class WebsiteCollect implements Runnable {

        private Collector Collector;

        public WebsiteCollect(Collector Collector) {
            super();
            this.Collector = Collector;
        }

        @Override
        public void run() {
            List<Proxy> draftproxys = Collector.newProxy(proxyRepository);
            ResourceFilter.filter(draftproxys);
            proxyService.save(beanMapper.mapAsList(draftproxys, ProxyModel.class));
        }

    }
}
