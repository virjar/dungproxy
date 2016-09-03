package com.virjar.scheduler;

import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.model.AvailbelCheckResponse;
import com.virjar.model.ProxyModel;
import com.virjar.service.ProxyService;
import com.virjar.utils.ProxyUtil;
import com.virjar.utils.ScoreUtil;
import com.virjar.utils.SysConfig;

@Component
public class AvailableValidater implements InitializingBean, Runnable {
    @Resource
    private ProxyService proxyService;

    @Resource
    private BeanMapper beanMapper;

    private static final Logger logger = LoggerFactory.getLogger(AvailableValidater.class);

    // 一般来说线程池不会有空转的,我希望所有线程能够随时工作,线程池除了节省线程创建和销毁开销,同时起限流作用,如果任务提交太多,则使用主线程进行工作
    // 从而阻塞主线程任务产生逻辑
    private ExecutorService pool = new ThreadPoolExecutor(SysConfig.getInstance().getAvailableCheckThread(),
            SysConfig.getInstance().getAvailableCheckThread(), 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy());

    private volatile boolean isRunning = false;

    @Override
    public void run() {
        long totalWaitTime;
        totalWaitTime = 10 * 60 * 1000;
        try {// 有效性检查模块延迟启动,因为tomcat环境可能没有启用,验证接口不能启用
            Thread.sleep(5 * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        isRunning = true;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                AvailableValidater.this.isRunning = false;
            }
        });
        logger.info("AvailableValidater start");
        while (isRunning) {
            try {
                List<ProxyModel> needupdate = proxyService.find4availableupdate();
                // logger.info("待跟新可用性资源数目:{},资源:{}", needupdate.size(), JSONObject.toJSON(needupdate));
                if (needupdate.size() == 0) {
                    logger.info("no proxy need to update");
                    try {// 大约8分钟
                        Thread.sleep(1 << 19);
                    } catch (InterruptedException e) {
                        logger.warn("thread sleep failed", e);
                    }
                    continue;
                }
                List<Future<Integer>> futures = Lists.newArrayList();
                for (ProxyModel proxy : needupdate) {
                    ProxyAvailableTester proxyAvailableTester = new ProxyAvailableTester(proxy);
                    futures.add(pool.submit(proxyAvailableTester));
                }

                long start = System.currentTimeMillis();
                for (Future<Integer> future : futures) {
                    try {
                        // 等待十分钟
                        future.get(totalWaitTime + start - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                // do nothing
                logger.error("error when check available", e);
            }
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(this).start();
    }

    private class ProxyAvailableTester implements Callable<Integer> {
        private ProxyModel proxy;

        ProxyAvailableTester(ProxyModel proxy) {
            super();
            this.proxy = proxy;
        }

        @Override
        public Integer call() {
            try {
                Long availbelScore = proxy.getAvailbelScore();
                long slot = ScoreUtil.calAvailableSlot(availbelScore);
                slot = slot == 0 ? 1 : slot;
                AvailbelCheckResponse response = ProxyUtil.validateProxyAvailable(proxy);
                if (response != null) {
                    proxy.setTransperent(response.getTransparent());
                    proxy.setProxyIp(response.getRemoteAddr());
                    if (availbelScore < 0) {// 不可用到可用,直接扭转,不用逐级升权
                        proxy.setAvailbelScore(1L);
                    } else {
                        proxy.setAvailbelScore(proxy.getAvailbelScore() + 1);
                    }
                    proxy.setConnectionScore(proxy.getConnectionScore() + 2);// 可用性验证本身包含连接性验证
                } else {
                    if (availbelScore < 0) {
                        proxy.setAvailbelScore(proxy.getAvailbelScore() - 1);
                    } else {
                        logger.warn("可用打分由可用转变为不可用 ip为:{}", JSONObject.toJSONString(proxy));
                        proxy.setAvailbelScore(
                                proxy.getAvailbelScore() - slot * SysConfig.getInstance().getAvaliableSlotFactory());
                    }
                }
                ProxyModel updateProxy = new ProxyModel();
                updateProxy.setAvailbelScore(proxy.getAvailbelScore());
                updateProxy.setId(proxy.getId());
                updateProxy.setAvailbelScoreDate(new Date());
                updateProxy.setConnectionScore(proxy.getConnectionScore());
                if (response != null) {
                    updateProxy.setSpeed(response.getSpeed());
                    updateProxy.setProxyIp(response.getRemoteAddr());
                    updateProxy.setTransperent(response.getTransparent());
                    if (response.getType() != null) {
                        updateProxy.setType(response.getType());
                    }
                }
                if (proxy.getIp().equals("202.106.16.36")) {
                    logger.info("tag:202.106.16.36,{}", JSONObject.toJSONString(updateProxy));
                }
                proxyService.updateByPrimaryKeySelective(updateProxy);
                return 0;
            } catch (Exception e) {
                logger.error("error when check available {}", JSONObject.toJSONString(proxy), e);
            }finally {
                try {
                    Thread.sleep(1000);//等待系统释放连接资源
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return 0;
        }
    }
}
