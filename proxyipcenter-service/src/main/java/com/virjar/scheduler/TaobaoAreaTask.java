package com.virjar.scheduler;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.google.common.util.concurrent.RateLimiter;
import com.virjar.entity.Proxy;
import com.virjar.repository.ProxyRepository;
import com.virjar.utils.net.HttpInvoker;
import com.virjar.utils.net.HttpResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by nicholas on 8/14/2016.
 */
public class TaobaoAreaTask implements Runnable, InitializingBean {

    private String TAOBAOURL = "http://ip.taobao.com/service/getIpInfo.php?ip=";

    ScheduledExecutorService getAreaThread = Executors.newScheduledThreadPool(1);

    private static final Logger logger = LoggerFactory.getLogger(TaobaoAreaTask.class);

    @Autowired
    private ProxyRepository proxyRepository;

    @Override
    public void run() {
        getAreaThread.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                for (long i = 1l; i <= 1000l; i++) {
                    setArea(i);
                }
            }
        }, 0, 1, TimeUnit.DAYS);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(this).start();
    }

    private Proxy getArea(String ipAddr) {
        RateLimiter limiter = RateLimiter.create(9.0);
        if (limiter.tryAcquire()) {
            HttpInvoker httpInvoker = new HttpInvoker(TAOBAOURL + ipAddr);
            HttpResult request;
            Proxy proxy;
            JSONObject jsonObject;
            try {
                request = httpInvoker.request();
                jsonObject = JSONObject.parseObject(request.getResponseBody());
                String data = jsonObject.get("data").toString();
                proxy = JSON.parseObject(data, Proxy.class, Feature.IgnoreNotMatch);
            } catch (Exception e) {
                logger.error("getAreaError" + e);
                proxy = new Proxy();
            }
            return proxy;
        } else {
            logger.info("QPS limit..." + ipAddr);
            return new Proxy();
        }
    }

    public int setArea(long id) {
        Proxy proxy = proxyRepository.selectByPrimaryKey(id);
        logger.info(proxy.toString());
        Proxy temp = getArea(proxy.getIp());
        temp.setId(id);
        int i = proxyRepository.updateByPrimaryKeySelective(temp);
        return i;
    }

    public static void main(String[] args) {
        System.out.println(JSONObject.toJSON(new TaobaoAreaTask().getArea("78.85.14.140")));
    }
}
