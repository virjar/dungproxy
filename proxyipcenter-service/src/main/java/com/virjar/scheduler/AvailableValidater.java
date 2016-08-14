package com.virjar.scheduler;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

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

    private ThreadPoolTaskExecutor executor;

    @Override
    public void run() {
        try {
            List<ProxyModel> needupdate = proxyService.find4availableupdate();
            if (needupdate.size() == 0) {
                logger.info("no proxy need to update");
                return;
            }
            for (ProxyModel proxy : needupdate) {
                executor.execute(new ProxyAvailableTester(proxy));
            }
        } catch (Exception e) {
            logger.error("error", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    private class ProxyAvailableTester implements Runnable {
        private ProxyModel proxy;

        public ProxyAvailableTester(ProxyModel proxy) {
            super();
            this.proxy = proxy;
        }

        @Override
        public void run() {
            if (proxy.getType() == null)
                return;
            Long availbelScore = proxy.getAvailbelScore();
            long slot = ScoreUtil.calAvailableSlot(availbelScore);
            slot = slot == 0 ? 1 : slot;
            long start = System.currentTimeMillis();
            if (ProxyUtil.validateProxyAvailable(proxy)) {
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
        }
    }
}
