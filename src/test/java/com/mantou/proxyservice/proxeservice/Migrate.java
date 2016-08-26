package com.mantou.proxyservice.proxeservice;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.google.common.util.concurrent.RateLimiter;
import com.virjar.scheduler.TaobaoAreaTask;
import com.virjar.utils.net.HttpInvoker;
import com.virjar.utils.net.HttpResult;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.data.domain.PageRequest;

import com.google.common.collect.Lists;
import com.mantou.proxyservice.proxeservice.entity.Proxy;
import com.mantou.proxyservice.proxeservice.repository.OldProxyRepository;
import com.virjar.utils.ProxyUtil;

/**
 * Created by virjar on 16/8/14.
 */
public class Migrate {

    private String TAOBAOURL = "http://ip.taobao.com/service/getIpInfo.php?ip=";

    public static void main(String[] args) {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "classpath:applicationContext.xml");
//        OldProxyRepository oldProxyRep = applicationContext.getBean(OldProxyRepository.class);
        com.virjar.repository.ProxyRepository newProxyRepo = applicationContext
                .getBean(com.virjar.repository.ProxyRepository.class);
//        int total = oldProxyRep.selectCount(new Proxy());
//        int batch = 10000;
//        int n = total / batch + 1;
//        ExecutorService pool = Executors.newFixedThreadPool(1000);
//        for (int i = 0; i < n; i++) {
//            System.out.println(i);
//            List<Future<Object>> futures = Lists.newArrayList();
//            List<Proxy> proxies = oldProxyRep.selectPage(new Proxy(), new PageRequest(i, batch));
//            for (Proxy proxy : proxies) {
//                futures.add(pool.submit(new insertTask(proxy, newProxyRepo)));
//            }
//            for (Future f : futures) {
//                try {
//                    f.get();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//        }
//        pool.shutdown();

        for (long i = 1L; i <= 1000l; i++) {
            try {
                new Migrate().setArea(newProxyRepo, i);
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    private static class insertTask implements Callable<Object> {

        private Proxy proxy;
        com.virjar.repository.ProxyRepository newProxyRepo;

        public insertTask(Proxy proxy, com.virjar.repository.ProxyRepository newProxyRepo) {
            this.proxy = proxy;
            this.newProxyRepo = newProxyRepo;
        }

        @Override
        public Object call() throws Exception {
            com.virjar.entity.Proxy newProxy = new com.virjar.entity.Proxy();
            newProxy.setIp(proxy.getIp());
            newProxy.setPort(proxy.getPort());
            newProxy.setIpValue(ProxyUtil.toIPValue(proxy.getIp()));
            newProxy.setConnectionScore(0L);
            newProxy.setAvailbelScore(0L);
            newProxyRepo.insert(newProxy);
            return this;
        }
    }


    private com.virjar.entity.Proxy getArea(String ipAddr) {
        RateLimiter limiter = RateLimiter.create(9.0);
        if (limiter.tryAcquire()) {
            HttpInvoker httpInvoker = new HttpInvoker(TAOBAOURL + ipAddr);
            HttpResult request;
            com.virjar.entity.Proxy proxy;
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
                proxy = new com.virjar.entity.Proxy();
            }
            return proxy;
        } else {
            return new com.virjar.entity.Proxy();
        }
    }

    public int setArea(com.virjar.repository.ProxyRepository newProxyRepo, long id) {
        com.virjar.entity.Proxy proxy = newProxyRepo.selectByPrimaryKey(id);
        com.virjar.entity.Proxy temp = getArea(proxy.getIp());
        temp.setId(proxy.getId());
        int i = newProxyRepo.updateByPrimaryKeySelective(temp);
        return i;
    }
}
