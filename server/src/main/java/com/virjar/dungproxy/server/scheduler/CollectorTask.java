package com.virjar.dungproxy.server.scheduler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.virjar.dungproxy.client.util.CommonUtil;
import com.virjar.dungproxy.server.core.beanmapper.BeanMapper;
import com.virjar.dungproxy.server.crawler.NewCollector;
import com.virjar.dungproxy.server.crawler.TemplateBuilder;
import com.virjar.dungproxy.server.entity.Proxy;
import com.virjar.dungproxy.server.model.ProxyModel;
import com.virjar.dungproxy.server.service.ProxyService;
import com.virjar.dungproxy.server.utils.NameThreadFactory;
import com.virjar.dungproxy.server.utils.ResourceFilter;
import com.virjar.dungproxy.server.utils.SysConfig;

@Component
public class CollectorTask implements Runnable, InitializingBean, ApplicationContextAware {

    @Resource
    private ProxyService proxyService;

    @Resource
    private BeanMapper beanMapper;

    private static final Logger logger = LoggerFactory.getLogger(CollectorTask.class);

    private boolean isRunning = false;

    // 一般来说线程池不会有空转的,我希望所有线程能够随时工作,线程池除了节省线程创建和销毁开销,同时起限流作用,如果任务提交太多,则使用主线程进行工作
    // 从而阻塞主线程任务产生逻辑
    private ExecutorService pool = null;

    public static List<NewCollector> getCollectors() {
        return newCollectors;
    }

    static List<NewCollector> newCollectors = Lists.newArrayList();

    @Override
    public void run() {
        logger.info("CollectorTask start");
        while (isRunning) {
            try {
                List<Future<Object>> futures = Lists.newArrayList();
                for (NewCollector collector : newCollectors) {
                    futures.add(pool.submit(new WebsiteCollect(collector)));
                }
                CommonUtil.waitAllFutures(futures);
                CommonUtil.sleep(20000);
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
        // init();

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, NewCollector> collectorMap = applicationContext.getBeansOfType(NewCollector.class);
        newCollectors.addAll(collectorMap.values());// 由spring管理的收集器
        newCollectors.addAll(TemplateBuilder.buildfromSource(null));// 模版类型的收集器
        init();
    }

    private class WebsiteCollect implements Callable<Object> {

        private NewCollector collector;

        public WebsiteCollect(NewCollector collector) {
            super();
            this.collector = collector;

        }

        @Override
        public Object call() throws Exception {
            List<Proxy> draftproxys = collector.newProxy();
            ResourceFilter.filter(draftproxys);
            proxyService.save(beanMapper.mapAsList(draftproxys, ProxyModel.class));
            return this;
        }
    }
}
