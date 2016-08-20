package com.virjar.scheduler;

import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

import javax.annotation.Resource;

import com.virjar.model.AvailbelCheckResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.virjar.core.beanmapper.BeanMapper;
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

    private ExecutorService pool = Executors.newFixedThreadPool(SysConfig.getInstance().getAvailableCheckThread());

    @Override
    public void run() {
        long totalWaitTime = 10 * 60 * 1000;
        try {//有效性检查模块延迟启动,因为tomcat环境可能没有启用,验证接口不能启用
            Thread.sleep(60 * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        logger.info("AvailableValidater start");
        while (true) {
            try {
                List<ProxyModel> needupdate = proxyService.find4availableupdate();
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
                    pool.submit(proxyAvailableTester);
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
            }
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(this).start();
    }

    private class SocketAvailableTester implements Callable{

        @Override
        public Object call() throws Exception {
            return null;
        }
    }

    private class ProxyAvailableTester implements Callable {
        private ProxyModel proxy;

        public ProxyAvailableTester(ProxyModel proxy) {
            super();
            this.proxy = proxy;
        }

        @Override
        public Integer call() throws Exception {
            if (proxy.getType() == null)
                return 0;
            Long availbelScore = proxy.getAvailbelScore();
            long slot = ScoreUtil.calAvailableSlot(availbelScore);
            slot = slot == 0 ? 1 : slot;
            long start = System.currentTimeMillis();
            AvailbelCheckResponse response = ProxyUtil.validateProxyAvailable(proxy);
            if (response != null) {
                proxy.setTransperent(response.getTransparent());
                proxy.setProxyIp(response.getRemoteAddr());
                if (availbelScore < 0) {
                    proxy.setAvailbelScore(proxy.getAvailbelScore() + slot * SysConfig.getInstance().getSlotFactory());
                } else {
                    proxy.setAvailbelScore(proxy.getAvailbelScore() + 1);
                }
            } else {
                if (availbelScore < 0) {
                    proxy.setAvailbelScore(proxy.getAvailbelScore() - 1);
                } else {
                    proxy.setAvailbelScore(proxy.getAvailbelScore() - slot * SysConfig.getInstance().getSlotFactory());
                }
            }
            ProxyModel updateProxy = new ProxyModel();
            updateProxy.setAvailbelScore(proxy.getAvailbelScore());
            updateProxy.setId(proxy.getId());
            updateProxy.setAvailbelScoreDate(new Date());
            updateProxy.setSpeed(System.currentTimeMillis() - start);
            proxyService.updateByPrimaryKeySelective(updateProxy);
            return 0;
        }
    }
}
