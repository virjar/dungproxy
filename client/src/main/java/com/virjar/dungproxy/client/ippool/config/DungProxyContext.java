package com.virjar.dungproxy.client.ippool.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.virjar.dungproxy.client.ippool.GroupBindRouter;
import com.virjar.dungproxy.client.ippool.PreHeater;
import com.virjar.dungproxy.client.ippool.strategy.*;
import com.virjar.dungproxy.client.ippool.strategy.impl.*;
import com.virjar.dungproxy.client.model.AvProxyVO;

/**
 * Created by virjar on 17/1/23.<br/>
 * 适用在整个项目的上下文
 *
 */
public class DungProxyContext {
    private AvProxyDumper avProxyDumper;
    private ProxyDomainStrategy needProxyStrategy;
    private String clientID;
    private GroupBindRouter groupBindRouter = new GroupBindRouter();
    private long feedBackDuration;
    private PreHeater preHeater = new PreHeater(this);
    private String serverBaseUrl;
    private long serializeStep;

    // for domain
    private Class<? extends ResourceFacade> defaultResourceFacade;
    private Class<? extends Offline> defaultOffliner;
    private Class<? extends Scoring> defaultScoring;
    private int defaultCoreSize;
    private double defaultSmartProxyQueueRatio;
    private long defaultUseInterval;
    private int defaultScoreFactory;
    private Map<String, DomainContext> domainConfig = Maps.newConcurrentMap();

    // 这个需要考虑并发安全吗?
    private Set<AvProxyVO> cloudProxySet = Sets.newConcurrentHashSet();

    private Logger logger = LoggerFactory.getLogger(DungProxyContext.class);

    /**
     * 加载全局的默认配置,统一加载后防止NPE
     */
    private void fillDefaultStrategy() {
        avProxyDumper = new JSONFileAvProxyDumper();
        needProxyStrategy = new WhiteListProxyStrategy();
        feedBackDuration = 1200000;// 20分钟一次 反馈
        defaultResourceFacade = DefaultResourceFacade.class;
        defaultOffliner = DefaultOffliner.class;
        defaultScoring = DefaultScoring.class;
        defaultCoreSize = 50;
        defaultSmartProxyQueueRatio = 0.3D;
        defaultUseInterval = 15000;// 默认IP15秒内不能重复使用
        defaultScoreFactory = 15;
        serverBaseUrl = "http://proxy.scumall.com:8080";
        serializeStep = 30;
        handleConfig();
    }

    public AvProxyDumper getAvProxyDumper() {
        return avProxyDumper;
    }

    public DungProxyContext setAvProxyDumper(AvProxyDumper avProxyDumper) {
        this.avProxyDumper = avProxyDumper;
        return this;
    }

    public String getClientID() {
        return clientID;
    }

    public DungProxyContext setClientID(String clientID) {
        this.clientID = clientID;
        return this;
    }

    public long getDefaultUseInterval() {
        return defaultUseInterval;
    }

    public DungProxyContext setDefaultUseInterval(long defaultUseInterval) {
        this.defaultUseInterval = defaultUseInterval;
        return this;
    }

    public int getDefaultCoreSize() {
        return defaultCoreSize;
    }

    public DungProxyContext setDefaultCoreSize(int defaultCoreSize) {
        this.defaultCoreSize = defaultCoreSize;
        return this;
    }

    public Class<? extends Offline> getDefaultOffliner() {
        return defaultOffliner;
    }

    public DungProxyContext setDefaultOffliner(Class<? extends Offline> defaultOffliner) {
        this.defaultOffliner = defaultOffliner;
        return this;
    }

    public Class<? extends ResourceFacade> getDefaultResourceFacade() {
        return defaultResourceFacade;
    }

    public DungProxyContext setDefaultResourceFacade(Class<? extends ResourceFacade> defaultResourceFacade) {
        this.defaultResourceFacade = defaultResourceFacade;
        return this;
    }

    public Class<? extends Scoring> getDefaultScoring() {
        return defaultScoring;
    }

    public DungProxyContext setDefaultScoring(Class<? extends Scoring> defaultScoring) {
        this.defaultScoring = defaultScoring;
        return this;
    }

    public double getDefaultSmartProxyQueueRatio() {
        return defaultSmartProxyQueueRatio;
    }

    public void setDefaultSmartProxyQueueRatio(double defaultSmartProxyQueueRatio) {
        this.defaultSmartProxyQueueRatio = defaultSmartProxyQueueRatio;
    }

    public Map<String, DomainContext> getDomainConfig() {
        return domainConfig;
    }

    public DungProxyContext addDomainConfig(DomainContext domainConfig) {
        this.domainConfig.put(domainConfig.getDomain(), domainConfig);
        domainConfig.extendWithDungProxyContext(this);
        return this;
    }

    public long getFeedBackDuration() {
        return feedBackDuration;
    }

    public DungProxyContext setFeedBackDuration(long feedBackDuration) {
        this.feedBackDuration = feedBackDuration;
        return this;
    }

    public GroupBindRouter getGroupBindRouter() {
        return groupBindRouter;
    }

    public DungProxyContext setGroupBindRouter(GroupBindRouter groupBindRouter) {
        this.groupBindRouter = groupBindRouter;
        return this;
    }

    public ProxyDomainStrategy getNeedProxyStrategy() {
        return needProxyStrategy;
    }

    public DungProxyContext setNeedProxyStrategy(ProxyDomainStrategy needProxyStrategy) {
        this.needProxyStrategy = needProxyStrategy;
        return this;
    }

    public PreHeater getPreHeater() {
        return preHeater;
    }

    public DungProxyContext setPreHeater(PreHeater preHeater) {
        this.preHeater = preHeater;
        return this;
    }

    public long getSerializeStep() {
        return serializeStep;
    }

    public DungProxyContext setSerializeStep(long serializeStep) {
        this.serializeStep = serializeStep;
        return this;
    }

    public String getServerBaseUrl() {
        return serverBaseUrl;
    }

    public DungProxyContext setServerBaseUrl(String serverBaseUrl) {
        this.serverBaseUrl = serverBaseUrl;
        return this;
    }

    public int getDefaultScoreFactory() {
        return defaultScoreFactory;
    }

    public DungProxyContext setDefaultScoreFactory(int defaultScoreFactory) {
        this.defaultScoreFactory = defaultScoreFactory;
        return this;
    }

    public DungProxyContext addCloudProxy(AvProxyVO cloudProxy) {
        this.cloudProxySet.add(cloudProxy);
        return this;
    }

    public Collection<AvProxyVO> getCloudProxies() {
        return Lists.newArrayList(cloudProxySet);// copy 新数据到外部
    }

    /**
     * 根据域名产生domain的schema
     * 
     * @return DomainContext
     */
    public DomainContext genDomainContext(String domain) {
        DomainContext domainContext = domainConfig.get(domain);
        if (domainContext != null) {
            return domainContext;
        }

        synchronized (DungProxyContext.class) {
            domainContext = domainConfig.get(domain);
            if (domainContext != null) {
                return domainContext;
            }
            domainConfig.put(domain, DomainContext.create(domain).extendWithDungProxyContext(this));
            return domainConfig.get(domain);
        }
    }

    public static DungProxyContext create() {
        DungProxyContext context = new DungProxyContext();
        context.fillDefaultStrategy();
        return context;
    }

    public DungProxyContext handleConfig() {
        if (defaultResourceFacade.isAssignableFrom(DefaultResourceFacade.class)) {
            DefaultResourceFacade.setAllAvUrl(serverBaseUrl + "/proxyipcenter/allAv");
            DefaultResourceFacade.setAvUrl(serverBaseUrl + "/proxyipcenter/av");
            DefaultResourceFacade.setFeedBackUrl(serverBaseUrl + "/proxyipcenter/feedBack");
            DefaultResourceFacade.setClientID(clientID);
        }
        return this;
    }

    public DungProxyContext buildDefaultConfigFile() {
        InputStream resourceAsStream = DungProxyContext.class.getClassLoader()
                .getResourceAsStream(ProxyConstant.CLIENT_CONFIG_FILE_NAME);
        if (resourceAsStream == null) {
            logger.warn("没有找到配置文件:{},代理规则几乎不会生效", ProxyConstant.CLIENT_CONFIG_FILE_NAME);
            return this;
        }
        Properties properties = new Properties();
        try {
            properties.load(resourceAsStream);
            return buildWithProperties(properties);
        } catch (IOException e) {
            logger.error("config file load error for file:{}", ProxyConstant.CLIENT_CONFIG_FILE_NAME, e);
        } finally {
            IOUtils.closeQuietly(resourceAsStream);
        }
        return this;
    }

    public DungProxyContext buildWithProperties(Properties properties) {
        if (properties == null) {
            return this;
        }

        // IP下载策略
        String resouceFace = properties.getProperty(ProxyConstant.RESOURCE_FACADE);
        if (StringUtils.isNotEmpty(resouceFace)) {
            defaultResourceFacade = ObjectFactory.classForName(resouceFace);
            String defaultResourceServerAddress = properties.getProperty(ProxyConstant.DEFAULT_RESOURCE_SERVER_ADDRESS);
            if (StringUtils.isNotEmpty(defaultResourceServerAddress)) {
                serverBaseUrl = defaultResourceServerAddress;
            }
        }

        // IP代理策略
        String proxyDomainStrategy = properties.getProperty(ProxyConstant.PROXY_DOMAIN_STRATEGY,
                ProxyConstant.DEFAULT_DOMAIN_STRATEGY);
        if ("WHITE_LIST".equalsIgnoreCase(proxyDomainStrategy)) {
            proxyDomainStrategy = WhiteListProxyStrategy.class.getName();
        } else if ("BLACK_LIST".equalsIgnoreCase(proxyDomainStrategy)) {
            proxyDomainStrategy = BlackListProxyStrategy.class.getName();
        }
        if (StringUtils.isNotEmpty(proxyDomainStrategy)) {
            needProxyStrategy = ObjectFactory.newInstance(proxyDomainStrategy);
            if (needProxyStrategy instanceof WhiteListProxyStrategy) {
                WhiteListProxyStrategy whiteListProxyStrategy = (WhiteListProxyStrategy) needProxyStrategy;
                String whiteListProperty = properties.getProperty(ProxyConstant.WHITE_LIST_STRATEGY);
                whiteListProxyStrategy.addAllHost(whiteListProperty);
            } else if (needProxyStrategy instanceof BlackListProxyStrategy) {
                BlackListProxyStrategy blackListProxyStrategy = (BlackListProxyStrategy) needProxyStrategy;
                String proxyDomainStrategyWhiteList = properties.getProperty(ProxyConstant.WHITE_LIST_STRATEGY);
                blackListProxyStrategy.addAllHost(proxyDomainStrategyWhiteList);
            }
        }

        // 反馈时间
        String feedBackDurationProperties = properties.getProperty(ProxyConstant.FEEDBACK_DURATION);
        if (StringUtils.isNoneEmpty(feedBackDurationProperties)) {
            feedBackDuration = NumberUtils.toLong(feedBackDurationProperties, 1200000);
        }

        // 序列化接口
        String avDumper = properties.getProperty(ProxyConstant.PROXY_SERIALIZER);
        if (StringUtils.isNotEmpty(avDumper)) {
            avProxyDumper = ObjectFactory.newInstance(avDumper);

        }
        String defaultAvDumpeFileName = properties.getProperty(ProxyConstant.DEFAULT_PROXY_SERALIZER_FILE);
        if (StringUtils.isNotEmpty(defaultAvDumpeFileName)) {
            avProxyDumper.setDumpFileName(defaultAvDumpeFileName);
        }


        String preHeaterTaskList = properties.getProperty(ProxyConstant.PREHEATER_TASK_LIST);
        if (StringUtils.isNotEmpty(preHeaterTaskList)) {
            for (String url : Splitter.on(",").split(preHeaterTaskList)) {
                preHeater.addTask(url);
            }
        }

        String preheaterSerilizeStep = properties.getProperty(ProxyConstant.PREHEAT_SERIALIZE_STEP);
        if (StringUtils.isNotEmpty(preheaterSerilizeStep)) {
            serializeStep = NumberUtils.toLong(preheaterSerilizeStep, 30L);
        }

        String proxyUseInterval = properties.getProperty(ProxyConstant.PROXY_USE_INTERVAL);
        if (StringUtils.isNotEmpty(proxyUseInterval)) {
            defaultUseInterval = NumberUtils.toLong(proxyUseInterval, 15000);
        }
        clientID = properties.getProperty(ProxyConstant.CLIENT_ID);
        String ruleRouter = properties.getProperty(ProxyConstant.PROXY_DOMAIN_STRATEGY_ROUTE);
        if (StringUtils.isNotEmpty(ruleRouter)) {
            groupBindRouter.buildCombinationRule(ruleRouter);
        }
        handleConfig();
        return this;
    }
}
