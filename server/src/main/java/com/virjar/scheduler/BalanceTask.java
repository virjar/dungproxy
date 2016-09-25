package com.virjar.scheduler;

import com.virjar.entity.Proxy;
import com.virjar.repository.ProxyLowQualityRepository;
import com.virjar.repository.ProxyRepository;
import com.virjar.utils.Constant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by nicholas on 9/19/2016.
 */
@Component
public class BalanceTask implements Runnable, InitializingBean {

    private static final Logger logger = LoggerFactory.getLogger(BalanceTask.class);

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private ProxyLowQualityRepository proxyLowQualityRepository;

    private static final int batchSize = 1000;
    private Integer maxPage = null;
    private Integer nowPage = 0;
    private volatile boolean isRuning = false;

    private boolean insertAndDelete(int step, int threshold) {
        if (maxPage == null || nowPage > maxPage) {
            int totalRecord = proxyRepository.selectCount(new Proxy());
            nowPage = 0;
            maxPage = totalRecord / batchSize + 1;
        }
        List<Proxy> lowProxy = proxyRepository.getLowProxy(step, threshold, new PageRequest(nowPage, batchSize));
        if (lowProxy == null || lowProxy.isEmpty()) {
            return true;
        }
        int insert = 0;
        int delete = 0;
        for (Proxy proxy : lowProxy) {
            insert += proxyLowQualityRepository.insertSelective(proxy);
            delete += proxyRepository.deleteByPrimaryKey(proxy.getId());
        }
        return insert == delete;
    }

    @Override
    public void run() {
        isRuning = false;
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                BalanceTask.this.isRuning = false;
            }
        });
        while (isRuning) {
            logger.info("begin migrate data....");
            boolean result = insertAndDelete(Constant.STEP, Constant.THRESHOLD);
            logger.info("end migrate data result = " + result);
            try {
                Thread.sleep(24 * 60 * 60 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(this).start();
    }

}
