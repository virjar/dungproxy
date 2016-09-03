package com.virjar.scheduler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

import javax.annotation.Resource;

import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.virjar.model.ProxyModel;
import com.virjar.service.ProxyService;
import com.virjar.utils.ProxyUtil;
import com.virjar.utils.ScoreUtil;
import com.virjar.utils.SysConfig;

@Component
public class ConnectionValidater implements Runnable, InitializingBean {

    @Resource
    private ProxyService proxyService;

    // 一般来说线程池不会有空转的,我希望所有线程能够随时工作,线程池除了节省线程创建和销毁开销,同时起限流作用,如果任务提交太多,则使用主线程进行工作
    // 从而阻塞主线程任务产生逻辑
    private ExecutorService pool = new ThreadPoolExecutor(SysConfig.getInstance().getConnectionCheckThread(),
            SysConfig.getInstance().getConnectionCheckThread(), 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy());

    private Logger logger = LoggerFactory.getLogger(ConnectionValidater.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(this).start();
    }

    public void run() {
        long totalWaitTime = 10 * 60 * 1000;
        logger.info("Component start");
        while (true) {
            try {
                // logger.info("begin connection check");
                List<ProxyModel> needupdate = proxyService.find4connectionupdate();
                if (needupdate.size() == 0) {
                    logger.info("no proxy need to update");
                    return;
                }
                List<Future<Object>> futures = Lists.newArrayList();
                for (ProxyModel proxy : needupdate) {
                    futures.add(pool.submit(new ProxyTester(proxy)));
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
                logger.error("error when check connection ", e);
            }
        }
    }

    private class ProxyTester implements Callable<Object> {
        private ProxyModel proxy;

        public ProxyTester(ProxyModel proxy) {
            super();
            this.proxy = proxy;
        }

        @Override
        public Object call() throws Exception {
            Long connectionScore = proxy.getConnectionScore();
            long slot = ScoreUtil.calAvailableSlot(connectionScore);
            slot = slot == 0 ? 1 : slot;
            try {
                Boolean aBoolean = ProxyUtil
                        .validateProxyConnect(new HttpHost(InetAddress.getByName(proxy.getIp()), proxy.getPort()));
                if (aBoolean == null) {
                    return this;
                }
                if (aBoolean) {
                    if (proxy.getConnectionScore() < 0) {// 不可用到可用,直接扭转,不用逐级升权
                        proxy.setConnectionScore(1L);
                    } else {
                        proxy.setConnectionScore(proxy.getConnectionScore() + 1);
                    }
                } else {
                    if (proxy.getConnectionScore() > 0) {
                        proxy.setConnectionScore(
                                proxy.getConnectionScore() - slot * SysConfig.getInstance().getAvaliableSlotFactory());
                        logger.warn("连接打分由可用转变为不可用 ip为:{}", JSONObject.toJSONString(proxy));
                    } else {
                        proxy.setConnectionScore(proxy.getConnectionScore() - 1);
                    }
                }
                ProxyModel updateProxy = new ProxyModel();
                updateProxy.setConnectionScore(proxy.getConnectionScore());
                updateProxy.setId(proxy.getId());
                updateProxy.setConnectionScoreDate(new Date());
                proxyService.updateByPrimaryKeySelective(updateProxy);
            } catch (UnknownHostException e) {
                logger.warn("ip不合法 {}", JSONObject.toJSONString(proxy), e);
                proxyService.deleteByPrimaryKey(proxy.getId());
            } finally {
               /* try {
                    Thread.sleep(1000);// 等待系统释放连接资源
                } catch (Exception e) {
                    //
                }*/
            }
            return this;
        }
    }
}
