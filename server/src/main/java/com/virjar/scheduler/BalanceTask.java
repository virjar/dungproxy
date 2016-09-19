package com.virjar.scheduler;

import com.virjar.entity.Proxy;
import com.virjar.repository.ProxyLowQualityRepository;
import com.virjar.repository.ProxyRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by nicholas on 9/19/2016.
 */
@Component
public class BalanceTask implements Runnable, InitializingBean {

    @Autowired
    private ProxyRepository proxyRepository;

    @Autowired
    private ProxyLowQualityRepository proxyLowQualityRepository;

    private boolean insertAndDelete(int step, int threshold) {
        List<Proxy> lowProxy = proxyRepository.getLowProxy(step, threshold);
        int insert = 0;
        int delete = 0;
        for (Proxy proxy : lowProxy)
            insert += proxyLowQualityRepository.insertSelective(proxy);
//        for (Proxy proxy : lowProxy)
//            delete += proxyRepository.deleteByPrimaryKey(proxy.getId());
        return insert == lowProxy.size();
    }

    public void repairData() {
//        for (int i = 7; i > 2; i--) {
//            insertAndDelete(i, -7);
//        }
        System.out.println(insertAndDelete(7,-7));
    }

    @Override
    public void run() {

    }

    @Override
    public void afterPropertiesSet() throws Exception {

    }

    public static void main(String[] args) {
        new BalanceTask().insertAndDelete(7, -7);
    }
}
