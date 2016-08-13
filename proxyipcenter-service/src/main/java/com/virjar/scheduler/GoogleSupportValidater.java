package com.virjar.scheduler;

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class GoogleSupportValidater implements Runnable {
    @Autowired
    private ProxyService proxyService;

    private static Logger logger = Logger.getLogger(GoogleSupportValidater.class);

    private ThreadPoolTaskExecutor executor;

    public ThreadPoolTaskExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(ThreadPoolTaskExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub
        try {
            List<Proxy> needupdate = proxyService.find4googleupdate();
            // needupdate.clear();
            if (needupdate.size() == 0) {
                logger.info("no proxy need to update");
                System.out.println("no proxy need to update");
                return;
            }
            for (Proxy proxy : needupdate) {
                executor.execute(new GoogleTester(proxy));
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            logger.error("error", e);
        }
    }

    private class GoogleTester implements Runnable {

        private Proxy proxy;

        public GoogleTester(Proxy proxy) {
            super();
            this.proxy = proxy;
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub
            HttpResult request = null;
            try {
                request = new HttpInvoker("https://www.google.com").setproxy(proxy.getIp(), proxy.getPort()).request();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                // e.printStackTrace();
            }
            if (request != null && request.getStatusCode() == 200) {
                if (proxy.getGooglesupport() < 0) {
                    proxy.setGooglesupport(proxy.getGooglesupport() + 2);
                } else {
                    proxy.setGooglesupport(proxy.getGooglesupport() + 1);
                }
            } else {
                if (proxy.getGooglesupport() > 0) {
                    proxy.setGooglesupport(proxy.getGooglesupport() - 2);
                } else {
                    proxy.setGooglesupport(proxy.getGooglesupport() - 1);
                }
            }
            Proxy updateProxy = new Proxy();
            updateProxy.setGooglesupport(proxy.getGooglesupport());
            updateProxy.setId(proxy.getId());

            proxyService.updateByPrimaryKeySelective(updateProxy);
        }

    }

    public static void main(String args[]) throws Exception {
        HttpResult request = new HttpInvoker("http://www.java1234.com").setproxy("211.23.19.130", 80).request();

        if (request != null && request.getResponseBody() != null) {
            System.out.print(new String(request.getResponseBody()));
        }
    }
}
