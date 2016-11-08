package com.mantou.proxyservice.proxeservice.proxyservice;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Description: ProxyserviceTest
 *
 * @author lingtong.fu
 * @version 2016-11-07 17:56
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/bean.xml", "classpath:applicationContext.xml"})
public class ProxyserviceTest {

    @Test
    public void test() {
        System.out.println("Hello World!");
    }
}
