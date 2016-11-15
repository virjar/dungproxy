package com.virjar.common.util;

/**
 * Description: 支持properties修改热加载
 * 注: 数据结构使用了 HashTable 重复key将被覆盖.
 *
 * @author lingtong.fu
 * @version 2016-11-15 13:33
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class PropertiesUtil {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);

    private static final int RELOAD_INTERVAL_SECONDS = 60;

    private static Properties properties = new Properties();

    private static final String[] DEFAULT_PROPERTIES_URL = {
            "proxyclient.properties"
    };

    static {
        for (String url : DEFAULT_PROPERTIES_URL) {
            loadFile(url);
        }
        startDeamon();
    }

    private static synchronized void loadFile(String fileUrl) {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(fileUrl);

        try {
            logger.info("重新加载配置文件");
            properties.load(is);
        } catch (IOException e) {
            logger.error("IOException when load" + fileUrl, e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                logger.error("IOException when close inputStream " + fileUrl, e);
            }
        }
    }

    private static void startDeamon() {
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    TimeUnit.SECONDS.sleep(RELOAD_INTERVAL_SECONDS);
                    for (String url : DEFAULT_PROPERTIES_URL) {
                        loadFile(url);
                    }
                } catch (InterruptedException e) {
                    logger.error("properties-deamon-thread Interrupted", e);
                }
            }

        }, "properties-deamon-thread");
        // 用户线程结束时,这个线程会立即中断
        t.setDaemon(true);
        t.start();
    }

    public static String getProperty(String key) {
        return getProperty(key, null);
    }

    public static synchronized String getProperty(String key, String defaultValue) {
        return properties.getProperty(key) == null ? defaultValue : properties.getProperty(key);
    }
}

