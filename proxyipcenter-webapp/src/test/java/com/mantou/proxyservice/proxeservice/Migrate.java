package com.mantou.proxyservice.proxeservice;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    public static void main(String[] args) {
        ClassPathXmlApplicationContext applicationContext = new ClassPathXmlApplicationContext(
                "classpath:applicationContext.xml");
        OldProxyRepository oldProxyRep = applicationContext.getBean(OldProxyRepository.class);
        com.virjar.repository.ProxyRepository newProxyRepo = applicationContext
                .getBean(com.virjar.repository.ProxyRepository.class);
        int total = oldProxyRep.selectCount(new Proxy());
        int batch = 10000;
        int n = total / batch + 1;
        ExecutorService pool = Executors.newFixedThreadPool(1000);
        for (int i = 0; i < n; i++) {
            System.out.println(i);
            List<Future<Object>> futures = Lists.newArrayList();
            List<Proxy> proxies = oldProxyRep.selectPage(new Proxy(), new PageRequest(i, batch));
            for (Proxy proxy : proxies) {
                futures.add(pool.submit(new insertTask(proxy, newProxyRepo)));
            }
            for (Future f : futures) {
                try {
                    f.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        pool.shutdown();
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
}
