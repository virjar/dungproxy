package com.virjar.dungproxy.client.ippool.config;

/**
 * Created by virjar on 16/9/30. 全局配置项
 */
public class ProxyConstant {
    public static final String USER_KEY = "VIRJAR_USER_KEY";
    public static final String PROXY_CONFIG_KEY = "VIRJAR_PROXY_CONFIG";
    public static final String USER_ENV_CONTAINER_KEY = "VIARJAR_USER_ENV_CONTANNER_KEY";
    public static final String USED_PROXY_KEY = "USED_PROXY_KEY";

    // config 文件默认配置key值
    public static final String RESOURCE_FACADE = "proxyclient.resouce.resourceFacade";
    public static final String DEFAULT_RESOURCE_FACADE = "com.virjar.ipproxy.ippool.strategy.resource.DefaultResourceFacade";
    public static final String PROXY_DOMAIN_STRATEGY = "proxyclient.proxyDomainStrategy";
    public static final String DEFAULT_DOMAIN_STRATEGY = "WHITE_LIST";
    public static final String BLACK_LIST_STRATEGY = "proxyclient.proxyDomainStrategy.backList";
    public static final String WHITE_LIST_STRATEGY = "proxyclient.proxyDomainStrategy.whiteList";
    public static final String FEEDBACK_DURATION = "proxyclient.feedback.duration";

    public static final String configFileName = "proxyclient.properties";
    public static final String PROXY_SERIALIZER = "proxyclient.serialize.serializer";
    public static final String DEFAULT_PROXY_SERIALIZER = "com.virjar.ipproxy.ippool.strategy.serialization.JSONFileAvProxyDumper";
    public static final String DEFAULT_PROXY_SERALIZER_FILE = "prxyclient.DefaultAvProxyDumper.dumpFileName";
    public static final String DEFAULT_PROXY_SERALIZER_FILE_VALUE = "availableProxy.json";

    // socket超时时间
    public static final int SOCKET_TIMEOUT = 30000;
    // 连接超时
    public static final int CONNECT_TIMEOUT = 30000;
    // 连接池分配连接超时时间,一般用处不大
    public static final int REQUEST_TIMEOUT = 30000;
    public static final int SOCKETSO_TIMEOUT = 15000;
}
