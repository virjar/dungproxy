package com.virjar.scheduler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.virjar.entity.Proxy;
import com.virjar.service.ProxyService;

public class ConnectionValidater implements Runnable {

    @Resource
    private ProxyService proxyService;

    private ThreadPoolTaskExecutor executor;

    private Logger logger = Logger.getLogger(ConnectionValidater.class);

    public ThreadPoolTaskExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ThreadPoolTaskExecutor executor) {
        this.executor = executor;
    }

    public void run() {

        try {
            List<Proxy> needupdate = proxyService.find4connectionupdate();
            if (needupdate.size() == 0) {
                logger.info("no proxy need to update");
                return;
            }
            for (Proxy proxy : needupdate) {
                executor.execute(new ProxyTester(proxy));
            }
        } catch (Exception e) {
            logger.error("error", e);
        }
    }

    private class ProxyTester implements Runnable {
        private Proxy proxy;

        public ProxyTester(Proxy proxy) {
            super();
            this.proxy = proxy;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            // now ignore socket and vpn
            if (proxy.getType() == null)
                return;
            try {
                if (ProxyUtil
                        .validateProxyConnect(new HttpHost(InetAddress.getByName(proxy.getIp()), proxy.getPort()))) {
                    if (proxy.getDirection() < 0) {
                        proxy.setDirection(1);
                        proxy.setStability(proxy.getStability() + 1);
                    }
                    if (proxy.getConnectionlevel() < 0) {
                        proxy.setConnectionlevel(1);
                    } else {
                        proxy.setConnectionlevel(proxy.getConnectionlevel() + 1);
                    }
                } else {
                    if (proxy.getDirection() > 0) {
                        proxy.setDirection(-1);
                        proxy.setStability(proxy.getStability() + 1);
                    }
                    if (proxy.getConnectionlevel() > 0) {
                        proxy.setConnectionlevel(proxy.getConnectionlevel() - 2);
                    } else {
                        proxy.setConnectionlevel(-1);
                    }
                }
                Proxy updateProxy = new Proxy();
                updateProxy.setConnectionlevel(proxy.getConnectionlevel());
                updateProxy.setId(proxy.getId());
                updateProxy.setDirection(proxy.getDirection());
                updateProxy.setStability(proxy.getStability());
                updateProxy.setLastupdate(new Date());
                proxyService.updateByPrimaryKeySelective(updateProxy);
            } catch (UnknownHostException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                proxyService.deleteByPrimaryKey(proxy.getId());
            }
        }
    }
}
