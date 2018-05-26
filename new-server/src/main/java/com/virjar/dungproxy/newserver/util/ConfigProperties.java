package com.virjar.dungproxy.newserver.util;

import com.google.common.base.Preconditions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by virjar on 2018/5/26.<br>
 * 读取配置文件
 */
public class ConfigProperties {
    private static Properties properties;

    static {
        loadConfig();
    }

    private static void loadConfig() {
        InputStream inputStream = ConfigProperties.class.getResourceAsStream("/config.properties");
        Preconditions.checkNotNull(inputStream, "can not find global config file \"config.properties\"");
        properties = new Properties(createDefaultConfig());
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException("failed to load global config", e);
        }
    }

    private static Properties createDefaultConfig() {
        return new Properties();
    }

    public static String getConfig(String key) {
        return properties.getProperty(key);
    }
}
