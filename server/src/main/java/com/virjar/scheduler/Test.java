package com.virjar.scheduler;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by nicholas on 9/19/2016.
 */
public class Test {

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext(
                "classpath:applicationContext.xml");

        //ProxyRepository bean = context.getBean(ProxyRepository.class);
        //ProxyLowQualityRepository bean1 = context.getBean(ProxyLowQualityRepository.class);

        BalanceTask bean = context.getBean(BalanceTask.class);
        bean.repairData();
    }
}
