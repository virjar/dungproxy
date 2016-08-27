package com.virjar.scheduler;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.google.common.util.concurrent.RateLimiter;
import com.virjar.entity.Proxy;
import com.virjar.repository.ProxyRepository;
import com.virjar.utils.net.HttpInvoker;
import com.virjar.utils.net.HttpResult;

/**
 * Created by nicholas on 8/14/2016.
 */
@Component
public class TaobaoAreaTask implements Runnable, InitializingBean {

    private static final String TAOBAOURL = "http://ip.taobao.com/service/getIpInfo.php?ip=";

    private ScheduledExecutorService getAreaThread = Executors.newScheduledThreadPool(1);

    private static final Logger logger = LoggerFactory.getLogger(TaobaoAreaTask.class);

    @Resource
    private ProxyRepository proxyRepository;

    private static final int batchSize = 1000;
    private Integer maxPage = null;
    private Integer nowPage = 0;
    private volatile boolean isRuning = false;
    private RateLimiter limiter = RateLimiter.create(8D);

    @Override
    public void run() {
        /*
         * getAreaThread.scheduleAtFixedRate(new Runnable() {
         * @Override public void run() { for (long i = 1l; i <= 1000l; i++) { setArea(i); } } }, 0, 1, TimeUnit.DAYS);
         */
        isRuning = true;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                TaobaoAreaTask.this.isRuning = false;
            }
        });

        while (isRuning) {
            List<Proxy> proxyList = find4Update();
            if (proxyList.size() == 0) {
                maxPage = null;
                continue;
            }
            for (Proxy proxy : proxyList) {
                Proxy area = getArea(proxy.getIp());
                area.setId(proxy.getId());
                if (StringUtils.isEmpty(area.getCountry()) && StringUtils.isEmpty(area.getArea())
                        && StringUtils.isEmpty(area.getIsp())) {
                    logger.warn("地址未知获取失败,response {},proxy:{}", JSONObject.toJSONString(area),
                            JSONObject.toJSONString(proxy));
                    continue;
                }
                proxyRepository.updateByPrimaryKeySelective(area);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(this).start();
    }

    private List<Proxy> find4Update() {
        if (maxPage == null || nowPage > maxPage) {
            // first run or reset page
            int totalRecord = proxyRepository.selectCount(new Proxy());
            nowPage = 0;
            maxPage = totalRecord / batchSize + 1;
        }
        List<Proxy> areaUpdate = proxyRepository.find4AreaUpdate(new PageRequest(nowPage, batchSize));
        nowPage++;
        return areaUpdate;
    }

    private Proxy getArea(String ipAddr) {
        if (limiter.tryAcquire(1, 1000, TimeUnit.MILLISECONDS)) {
            HttpInvoker httpInvoker = new HttpInvoker(TAOBAOURL + ipAddr);
            HttpResult request;
            Proxy proxy;
            JSONObject jsonObject;
            try {
                request = httpInvoker.request();
                jsonObject = JSONObject.parseObject(request.getResponseBody());
                String data = jsonObject.get("data").toString();
                JSONObject temp = JSONObject.parseObject(data);
                proxy = JSON.parseObject(data, com.virjar.entity.Proxy.class, Feature.IgnoreNotMatch);
                proxy.setCountryId(temp.get("country_id").toString());
                proxy.setAreaId(temp.get("area_id").toString());
                proxy.setRegionId(temp.get("region_id").toString());
                proxy.setCityId(temp.get("city_id").toString());
                proxy.setIspId(temp.get("isp_id").toString());
            } catch (Exception e) {
                logger.error("getAreaError" + e);
                proxy = new Proxy();
            }
            return proxy;
        } else {
            logger.info("QPS limit..." + ipAddr);
            return getArea(ipAddr);
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
