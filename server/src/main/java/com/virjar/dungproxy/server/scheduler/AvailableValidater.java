package com.virjar.dungproxy.server.scheduler;

import java.util.Date;
import java.util.List;
import java.util.concurrent.*;

import javax.annotation.Resource;

import com.virjar.dungproxy.client.util.CommonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.virjar.dungproxy.server.core.beanmapper.BeanMapper;
import com.virjar.dungproxy.server.model.AvailbelCheckResponse;
import com.virjar.dungproxy.server.model.ProxyModel;
import com.virjar.dungproxy.server.service.ProxyService;
import com.virjar.dungproxy.server.utils.NameThreadFactory;
import com.virjar.dungproxy.server.utils.ProxyUtil;
import com.virjar.dungproxy.server.utils.SysConfig;

@Component
public class AvailableValidater implements InitializingBean, Runnable {
    @Resource
    private ProxyService proxyService;

    @Resource
    private BeanMapper beanMapper;

    private static final Logger logger = LoggerFactory.getLogger(AvailableValidater.class);

    private ExecutorService pool = null;

    private volatile boolean isRunning = false;

    private void init() {
        isRunning = SysConfig.getInstance().getAvailableCheckThread() > 0;
        if (!isRunning) {
            logger.info("avaliable validater is not enable");
            return;
        }
        pool = new ThreadPoolExecutor(SysConfig.getInstance().getAvailableCheckThread(),
                SysConfig.getInstance().getAvailableCheckThread(), 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new NameThreadFactory("available-check"),
                new ThreadPoolExecutor.CallerRunsPolicy());
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {// 有效性检查模块延迟启动,因为tomcat环境可能没有启用,验证接口不能启用
            Thread.sleep(5 * 1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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
                    CommonUtil.sleep(1 << 19);
                    continue;
                }
                List<Future<Integer>> futures = Lists.newArrayList();
                for (ProxyModel proxy : needupdate) {
                    ProxyAvailableTester proxyAvailableTester = new ProxyAvailableTester(proxy);
                    futures.add(pool.submit(proxyAvailableTester));
                }
                CommonUtil.waitAllFutures(futures);
            } catch (Exception e) {
                // do nothing
                logger.error("error when check available", e);
            }
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
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
                    if (availbelScore <= 0) {// 等于0的时候,也会出现在这里,
                        proxy.setAvailbelScore(proxy.getAvailbelScore() - 1);
                    } else {
                        // 通过这里保证灵敏性,对于高质量资源,如果探测到不可用,那么将会快速降权。降权因子根据当前打分决定(分值其实也是权重,分值越高排序越考前),
                        // 降权分值复合对数函数,保证站得越高,摔的越快,但是又不能是和当前分值成线性关系。所以找了一个对数函数来降权。
                        // 计算降权分值的时候,在原分值上面加3的原因是这里对数是以e为底的对数(e=2.71828),这样算出来的分值必须大于等于1,小于1起不到降权效果
                        // 计算机是离散的,
                        proxy.setAvailbelScore(proxy.getAvailbelScore() - (long) Math.log((double) availbelScore + 3));
                        logger.warn("可用打分由可用转变为不可用 preScore:{} ip为:{}", availbelScore, JSONObject.toJSONString(proxy));
                    }
                }
                ProxyModel updateProxy = new ProxyModel();
                updateProxy.setAvailbelScore(proxy.getAvailbelScore());
                updateProxy.setId(proxy.getId());
                updateProxy.setAvailbelScoreDate(new Date());
                updateProxy.setConnectionScore(proxy.getConnectionScore());
                if (response != null) {
                    if (proxy.getSpeed() == null) {
                        proxy.setSpeed(0L);
                    }
                    updateProxy.setLostheader(response.isLostHeader());
                    updateProxy.setSpeed((proxy.getSpeed() * 9 + response.getSpeed()) / 10);
                    updateProxy.setProxyIp(response.getRemoteAddr());
                    updateProxy.setTransperent(response.getTransparent());
                    if (response.getType() != null) {
                        updateProxy.setType(response.getType());
                    }
                }
                proxyService.updateByPrimaryKeySelective(updateProxy);
                return 0;
            } catch (Exception e) {
                logger.error("error when check available {}", JSONObject.toJSONString(proxy), e);
            } finally {
                /*
                 * try { Thread.sleep(1000);//等待系统释放连接资源 } catch (InterruptedException e) { e.printStackTrace(); }
                 */
            }
            return 0;
        }
    }
}
