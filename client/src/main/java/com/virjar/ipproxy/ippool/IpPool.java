package com.virjar.ipproxy.ippool;

import java.util.Map;

import com.virjar.ipproxy.ippool.strategy.resource.ResourceFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.virjar.ipproxy.ippool.config.Context;
import com.virjar.ipproxy.ippool.config.ObjectFactory;
import com.virjar.model.AvProxy;
import com.virjar.ipproxy.util.CommonUtil;

/**
 * Description: IpListPool
 *
 * @author lingtong.fu
 * @version 2016-09-03 01:13
 */
public class IpPool {

    private Logger logger = LoggerFactory.getLogger(IpPool.class);

    private Map<String, DomainPool> pool = Maps.newConcurrentMap();

    private IpPool() {
        init();
    }

    private void init() {
        new FeedBackThread().start();
        new FreshResourceThread().start();
    }

    private static IpPool instance = new IpPool();

    public static IpPool getInstance() {
        return instance;
    }

    public AvProxy bind(String host, String url, Object userID) {
        if (!pool.containsKey(host)) {
            synchronized (this) {
                if (!pool.containsKey(host)) {
                    String importer = Context.getInstance().getResourceFacade();
                    pool.put(host, new DomainPool(host, ObjectFactory.<ResourceFacade> newInstance(importer)));
                }
            }
        }
        return pool.get(host).bind(url, userID);
    }

    // 向服务器反馈不可用IP
    private class FeedBackThread extends Thread {
        @Override
        public void run() {
            while (true) {
                for (DomainPool domainPool : pool.values()) {
                    try {
                        domainPool.feedBack();
                    } catch (Exception e) {
                        logger.error("ip feedBack error for domain:{}", domainPool.getDomain(), e);
                    }
                }
                CommonUtil.sleep(Context.getInstance().getFeedBackDuration());
            }
        }
    }

    // 检查IP池资源是否足够,不够则启动线程下载资源
    private class FreshResourceThread extends Thread {
        @Override
        public void run() {
            while (true) {
                for (DomainPool domainPool : pool.values()) {
                    try {
                        if (domainPool.needFresh()) {
                            domainPool.fresh();
                        }
                    } catch (Exception e) {
                        logger.error("error when fresh ip pool for domain:{}", domainPool.getDomain());
                    }
                }
                CommonUtil.sleep(2000);
            }
        }
    }
}
