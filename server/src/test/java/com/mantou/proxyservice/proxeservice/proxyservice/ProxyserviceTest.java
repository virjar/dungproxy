package com.mantou.proxyservice.proxeservice.proxyservice;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import com.virjar.client.proxyclient.ProxyClient;
import com.virjar.client.proxyclient.VirjarAsyncClient;
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

    private static final String url = "http://www.xroxy.com/";
    //private static final VirjarAsyncClient client = new VirjarAsyncClient();
    private static final long MAX_CRAWL_TIMEOUT = 60000;

    @Test
    public void test() {
        log.info("========================= Start test ========================= ");
        try {
            //MAX_CRAWL_TIMEOUT, TimeUnit.MILLISECONDS
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
