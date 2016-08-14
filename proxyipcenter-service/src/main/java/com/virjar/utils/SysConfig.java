package com.virjar.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class SysConfig {

    private String keyverifyurl;
    private int validateBatchSize;
    private String validateBatchRatio;
    private int slotNumber;
    private int slotFactory;

    private SysConfig() {
        load();
    }

    private static SysConfig instance;
    static {
        instance = new SysConfig();
    }

    public static SysConfig getInstance() {
        return instance;
    }

    public void load() {
        InputStream resourceAsStream = null;
        try {
            Properties properties = new Properties();
            resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");
            properties.load(resourceAsStream);
            keyverifyurl = properties.getProperty("system.availablecheck.url");
            validateBatchSize = NumberUtils.toInt(properties.getProperty("system.validateBatchSize"), 768);
            validateBatchRatio = properties.getProperty("system.validateBatchRatio", "1:1");
            slotFactory = NumberUtils.toInt(properties.getProperty("system.slotNumber"));
            slotNumber = NumberUtils.toInt(properties.getProperty("system.slotFactory"));
        } catch (IOException e) {
            throw new IllegalStateException("配置文件加载失败,系统不能启动", e);
        } finally {
            IOUtils.closeQuietly(resourceAsStream);
        }
    }

    public String getValidateBatchRatio() {
        return validateBatchRatio;
    }

    public int getValidateBatchSize() {
        return validateBatchSize;
    }

    public int getSlotFactory() {
        return slotFactory;
    }

    public int getSlotNumber() {
        return slotNumber;
    }

    public String getKeyverifyurl() {
        return keyverifyurl;
    }
}
