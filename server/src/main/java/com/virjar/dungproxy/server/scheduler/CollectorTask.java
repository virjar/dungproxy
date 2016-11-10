package com.virjar.dungproxy.server.scheduler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.virjar.dungproxy.client.util.CommonUtil;
import com.virjar.dungproxy.server.core.beanmapper.BeanMapper;
import com.virjar.dungproxy.server.crawler.Collector;
import com.virjar.dungproxy.server.entity.Proxy;
import com.virjar.dungproxy.server.model.ProxyModel;
import com.virjar.dungproxy.server.service.ProxyService;
import com.virjar.dungproxy.server.utils.NameThreadFactory;
import com.virjar.dungproxy.server.utils.ResourceFilter;
import com.virjar.dungproxy.server.utils.SysConfig;

@Component
public class CollectorTask implements Runnable, InitializingBean {

    @Resource
    private ProxyService proxyService;

    @Resource
    private BeanMapper beanMapper;

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
        logger.info("CollectorTask start");
        while (isRunning) {
            try {
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
            List<Proxy> draftproxys = collector.newProxy();
            logger.info("收集器:{} 收集的资源数目:{}", collector.getLastUrl(), draftproxys.size());
            // logger.info("收集到的新资源:{}", JSON.toJSONString(draftproxys));
            ResourceFilter.filter(draftproxys);
            proxyService.save(beanMapper.mapAsList(draftproxys, ProxyModel.class));
            return this;
        }
    }
}
