package com.virjar.scheduler.commontask;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.virjar.ipproxy.util.CommonUtil;
import com.virjar.utils.NameThreadFactory;
import com.virjar.utils.SysConfig;

/**
 * Created by virjar on 16/10/3.
 */
@Component
public class CommonTaskThreadPool implements InitializingBean, Runnable, ApplicationContextAware {
    private static final Logger logger = LoggerFactory.getLogger(CommonTaskThreadPool.class);
    private ExecutorService pool = null;

    private volatile boolean isRunning = false;

    private List<CommonTask> taskList = Lists.newArrayList();

    private void init() {
        isRunning = SysConfig.getInstance().getCommonTaskThread() > 0;
        if (!isRunning) {
            logger.info("common task thread pool is not enable");
            return;
        }
        pool = new ThreadPoolExecutor(SysConfig.getInstance().getCommonTaskThread(),
                SysConfig.getInstance().getCommonTaskThread(), 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new NameThreadFactory("common-task"),
                new ThreadPoolExecutor.CallerRunsPolicy());
        new Thread(this).start();
    }

    @Override
    public void run() {
        while (isRunning) {
            for (CommonTask task : taskList) {
                try {
                    logger.info("execute task:{}", task.getClass());
                    task.execute();
                } catch (Exception e) {
                    logger.error("error when execute task:{}", task.getClass(), e);
                }
            }
            CommonUtil.sleep(1000);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, CommonTask> beansOfType = applicationContext.getBeansOfType(CommonTask.class);
        this.taskList.addAll(beansOfType.values());
    }
}
