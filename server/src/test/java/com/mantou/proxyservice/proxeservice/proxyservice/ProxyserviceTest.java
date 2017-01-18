package com.mantou.proxyservice.proxeservice.proxyservice;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import com.virjar.dungproxy.client.ningclient.proxyclient.ProxyClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.Future;

/**
 * Description: ProxyserviceTest
 *
 * @author lingtong.fu
 * @version 2016-11-07 17:56
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext.xml"})
public class ProxyserviceTest {

    private static final Logger log = LoggerFactory.getLogger(ProxyserviceTest.class);

    private static final String url = "http://www.kuaidaili.com/";

    @Test
    public void test() {
        log.info("========================= Start test ========================= ");
        try {
            String content = baiduTest().get();

            log.info("========================= Content is: {} ========================= ", content);
        } catch (Exception e) {
            log.info("========================= e: ========================= ", e);
        }
    }

    public Future<String> baiduTest() {
        try {
            ProxyClient proxyClient = new ProxyClient();
            return proxyClient.get(url, new AsyncCompletionHandler<String>() {
                @Override
                public String onCompleted(Response response) throws Exception {
                    if (response.getStatusCode() == 200) {
                        return response.getResponseBody();
                    }
                    return null;
                }
            });
        } catch (Exception e) {
            log.info("========================= e: ========================= ", e);
        }
        return null;
    }
}
