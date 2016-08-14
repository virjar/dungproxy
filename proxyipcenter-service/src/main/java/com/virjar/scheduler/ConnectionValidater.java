package com.virjar.scheduler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.http.HttpHost;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import com.virjar.model.ProxyModel;
import com.virjar.service.ProxyService;
import com.virjar.utils.ProxyUtil;
import com.virjar.utils.ScoreUtil;
import com.virjar.utils.SysConfig;

@Component
public class ConnectionValidater implements Runnable, InitializingBean {

    @Resource
    private ProxyService proxyService;

    private ThreadPoolTaskExecutor executor;

    private Logger logger = Logger.getLogger(ConnectionValidater.class);

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    public void run() {

        try {
            List<ProxyModel> needupdate = proxyService.find4connectionupdate();
            if (needupdate.size() == 0) {
                logger.info("no proxy need to update");
                return;
            }
            for (ProxyModel proxy : needupdate) {
                executor.execute(new ProxyTester(proxy));
            }
        } catch (Exception e) {
            logger.error("error", e);
        }
    }

    private class ProxyTester implements Runnable {
        private ProxyModel proxy;

        public ProxyTester(ProxyModel proxy) {
            super();
            this.proxy = proxy;
        }

        @Override
        public void run() {
            if (proxy.getType() == null)
                return;
            Long connectionScore = proxy.getConnectionScore();
            long slot = ScoreUtil.calAvailableSlot(connectionScore);
            slot = slot == 0 ? 1 : slot;
            try {
                if (ProxyUtil
                        .validateProxyConnect(new HttpHost(InetAddress.getByName(proxy.getIp()), proxy.getPort()))) {
                    if (proxy.getConnectionScore() < 0) {
                        proxy.setConnectionScore(
                                proxy.getConnectionScore() + slot * SysConfig.getInstance().getSlotFactory());
                    } else {
                        proxy.setConnectionScore(proxy.getConnectionScore() + 1);
                    }
                } else {
                    if (proxy.getConnectionScore() > 0) {
                        proxy.setConnectionScore(
                                proxy.getConnectionScore() - slot * SysConfig.getInstance().getSlotFactory());
                    } else {
                        proxy.setConnectionScore(proxy.getConnectionScore() + 1);
                    }
                }
                ProxyModel updateProxy = new ProxyModel();
                updateProxy.setConnectionScore(proxy.getConnectionScore());
                updateProxy.setId(proxy.getId());
                updateProxy.setConnectionScoreDate(new Date());
                proxyService.updateByPrimaryKeySelective(updateProxy);
            } catch (UnknownHostException e) {
                proxyService.deleteByPrimaryKey(proxy.getId());
            }
        }
    }
}
