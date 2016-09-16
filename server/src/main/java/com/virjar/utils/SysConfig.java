package com.virjar.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class SysConfig {

    private String keyverifyurl;

    private int avaliableValidateBatchSize;
    private String avaliableValidateBatchRatio;
    private int avaliableSlotNumber;
    private int avaliableSlotFactory;

    private int connectionValidateBatchSize;
    private String connectionValidateBatchRatio;
    private int connectionSlotNumber;
    private int connectionSlotFactory;

    private int availableCheckThread;
    private int connectionCheckThread;
    private int addreddSyncThread;
    private int gfwSupportCheckThread;
    private int ipCrawlerThread;
    private int portCheckThread;
    private int domainCheckThread;

    private boolean availableEnable = true;
    private boolean connectionEnable = true;
    private boolean ipCrawlerEnable = true;
    private boolean portCheckEnable = true;
    private boolean domainCheckEnable = true;

    private SysConfig() {
        load();
    }

    private static SysConfig instance;
    static {
        instance = new SysConfig();
    }

    private Properties properties;

    public static SysConfig getInstance() {
        return instance;
    }

    public void load() {
        InputStream resourceAsStream = null;
        try {
            Properties properties = new Properties();
            resourceAsStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");
            properties.load(resourceAsStream);
            this.properties = properties;
            keyverifyurl = properties.getProperty("system.availablecheck.url");
            avaliableValidateBatchSize = NumberUtils.toInt(properties.getProperty("avaliable.validateBatchSize"), 768);
            avaliableValidateBatchRatio = properties.getProperty("avaliable.validateBatchRatio", "1:1");
            avaliableSlotFactory = NumberUtils.toInt(properties.getProperty("avaliable.slotFactory"));
            avaliableSlotNumber = NumberUtils.toInt(properties.getProperty("avaliable.slotNumber"));

            connectionValidateBatchSize = NumberUtils.toInt(properties.getProperty("connection.validateBatchSize"),
                    768);
            connectionValidateBatchRatio = properties.getProperty("connection.validateBatchRatio", "1:1");
            connectionSlotFactory = NumberUtils.toInt(properties.getProperty("connection.slotFactory"));
            connectionSlotNumber = NumberUtils.toInt(properties.getProperty("connection.slotNumber"));

            availableCheckThread = NumberUtils.toInt(properties.getProperty("system.thread.availableCheckThread"), 2);
            connectionCheckThread = NumberUtils.toInt(properties.getProperty("system.thread.connectionCheckThread"),
                    20);
            addreddSyncThread = NumberUtils.toInt(properties.getProperty("system.thread.addreddSyncThread"), 5);
            gfwSupportCheckThread = NumberUtils.toInt(properties.getProperty("system.thread.gfwSupportCheckThread"), 5);
            ipCrawlerThread = NumberUtils.toInt(properties.getProperty("system.thread.ipCrawlerThread"), 2);
            portCheckThread = NumberUtils.toInt(properties.getProperty("system.thread.portCheckThread"), 10);
            domainCheckThread = NumberUtils.toInt(properties.getProperty("system.thread.domainCheckThread"), 5);

            availableEnable = "true".equalsIgnoreCase(properties.getProperty("system.component.availablecheck.enable"));
            connectionEnable = "true".equalsIgnoreCase(properties.getProperty("system.component.connection.enable"));
            portCheckEnable = "true".equalsIgnoreCase(properties.getProperty("system.component.portcheck.enable"));
            domainCheckEnable = "true".equalsIgnoreCase(properties.getProperty("system.component.domaincheck.enable"));
            ipCrawlerEnable = "true".equalsIgnoreCase(properties.getProperty("system.component.ipcrawler.enable"));
        } catch (IOException e) {
            throw new IllegalStateException("配置文件加载失败,系统不能启动", e);
        } finally {
            IOUtils.closeQuietly(resourceAsStream);
        }
    }

    public String getAvaliableValidateBatchRatio() {
        return avaliableValidateBatchRatio;
    }

    public int getAvaliableValidateBatchSize() {
        return avaliableValidateBatchSize;
    }

    public int getAvaliableSlotFactory() {
        return avaliableSlotFactory;
    }

    public int getAvaliableSlotNumber() {
        return avaliableSlotNumber;
    }

    public int getConnectionSlotFactory() {
        return connectionSlotFactory;
    }

    public int getConnectionSlotNumber() {
        return connectionSlotNumber;
    }

    public String getConnectionValidateBatchRatio() {
        return connectionValidateBatchRatio;
    }

    public int getConnectionValidateBatchSize() {
        return connectionValidateBatchSize;
    }

    public String getKeyverifyurl() {
        return keyverifyurl;
    }

    public int getAddreddSyncThread() {
        return addreddSyncThread;
    }

    public int getAvailableCheckThread() {
        return availableCheckThread;
    }

    public int getConnectionCheckThread() {
        return connectionCheckThread;
    }

    public int getGfwSupportCheckThread() {
        return gfwSupportCheckThread;
    }

    public int getIpCrawlerThread() {
        return ipCrawlerThread;
    }

    public String getPropertis(String key) {
        return properties.getProperty(key);
    }

    public boolean recordFaildResponse() {
        return StringUtils.equalsIgnoreCase("true", properties.getProperty("system.record_fail_response=false"));
    }

    public int getPortCheckThread() {
        return portCheckThread;
    }

    public int getDomainCheckThread() {
        return domainCheckThread;
    }

    public boolean isAvailableEnable() {
        return availableEnable;
    }

    public boolean isConnectionEnable() {
        return connectionEnable;
    }

    public boolean isDomainCheckEnable() {
        return domainCheckEnable;
    }

    public boolean isIpCrawlerEnable() {
        return ipCrawlerEnable;
    }

    public boolean isPortCheckEnable() {
        return portCheckEnable;
    }
}
