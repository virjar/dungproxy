package com.virjar.dungproxy.server.scheduler;

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
import com.virjar.dungproxy.client.util.CommonUtil;
import com.virjar.dungproxy.server.model.ProxyModel;
import com.virjar.dungproxy.server.service.ProxyService;
import com.virjar.dungproxy.server.utils.NameThreadFactory;
import com.virjar.dungproxy.server.utils.ProxyUtil;
import com.virjar.dungproxy.server.utils.SysConfig;

@Component
public class ConnectionValidater implements Runnable, InitializingBean {

    @Resource
    private ProxyService proxyService;

    private boolean isRunning = false;

    private ExecutorService pool = null;

    private Logger logger = LoggerFactory.getLogger(ConnectionValidater.class);

    private void init() {
        isRunning = SysConfig.getInstance().getConnectionCheckThread() > 0;
        if (!isRunning) {
            logger.info("connection validator is not running");
            return;
        }
        pool = new ThreadPoolExecutor(SysConfig.getInstance().getConnectionCheckThread(),
                SysConfig.getInstance().getConnectionCheckThread(), 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new NameThreadFactory("connection-check"),
                new ThreadPoolExecutor.CallerRunsPolicy());
        new Thread(this).start();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();

    }

    public void run() {
        long totalWaitTime = 100 * 60 * 1000;
        logger.info("Component start");

        while (isRunning) {
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
                CommonUtil.waitAllFutures(futures);
                Thread.sleep(9000);// 等待9秒钟,用于系统释放套接字资源
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
                        if (proxy.getConnectionScore() > SysConfig.getInstance().getConnectionMaxScore()) {
                            proxy.setConnectionScore(SysConfig.getInstance().getConnectionMaxScore());
                        }
                    }
                } else {
                    if (proxy.getConnectionScore() > 0) {
                        long preScore = proxy.getConnectionScore();
                        proxy.setConnectionScore(
                                proxy.getConnectionScore() - (int) Math.log((double) proxy.getConnectionScore() + 3));
                        logger.warn("连接打分由可用转变为不可用 prescore:{}  ip为:{}", preScore, JSONObject.toJSONString(proxy));
                    } else {
                        proxy.setConnectionScore(proxy.getConnectionScore() - 1);
                        if (proxy.getConnectionScore() < SysConfig.getInstance().getConnectionMinScore()) {
                            proxy.setConnectionScore(SysConfig.getInstance().getConnectionMinScore());
                        }
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
                /*
                 * try { Thread.sleep(1000);// 等待系统释放连接资源 } catch (Exception e) { // }
                 */
            }
            return this;
        }
    }
}
