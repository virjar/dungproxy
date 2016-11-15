package com.virjar.dungproxy.client.ippool.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.virjar.dungproxy.client.ippool.PreHeater;
import com.virjar.dungproxy.client.ippool.strategy.offline.Offline;
import com.virjar.dungproxy.client.ippool.strategy.proxydomain.BlackListProxyStrategy;
import com.virjar.dungproxy.client.ippool.strategy.proxydomain.ProxyDomainStrategy;
import com.virjar.dungproxy.client.ippool.strategy.proxydomain.WhiteListProxyStrategy;
import com.virjar.dungproxy.client.ippool.strategy.serialization.AvProxyDumper;
import com.virjar.dungproxy.client.ippool.strategy.serialization.JSONFileAvProxyDumper;
import com.virjar.dungproxy.client.model.DefaultProxy;
import com.virjar.dungproxy.client.util.IpAvValidator;

/**
 * client配置 Created by virjar on 16/9/30.
 */
public class Context {
    // IP资源引入器 一般不需要修改
    private String resourceFacade;

    // 代理网站过滤器,通过这个类确认哪些网站需要进行代理
    private ProxyDomainStrategy needProxyStrategy;

    private Offline offliner;

    private int feedBackDuration;

    private AvProxyDumper avProxyDumper;

    private static final Logger logger = LoggerFactory.getLogger(Context.class);

    public ProxyDomainStrategy getNeedProxyStrategy() {
        return needProxyStrategy;
    }

    public String getResourceFacade() {
        return resourceFacade;
    }

    private PreHeater preHeater = new PreHeater();

    private List<DefaultProxy> defaultProxyList = Lists.newArrayList();

    private List<String> preHeaterTaskList;

    private int scoreFactory = 10;

    private int minActivityTime = 10* 60 * 1000;//一个IP如果超过10分钟没有被使用

    private Context() {
    }

    public AvProxyDumper getAvProxyDumper() {
        return avProxyDumper;
    }

    // 唯一持有的对象,存储策略
    private static Context instance;
    private static volatile boolean hasInit = false;

    public int getScoreFactory() {
        return scoreFactory;
    }

    public Offline getOffliner() {
        return offliner;
    }

    public int getFeedBackDuration() {
        return feedBackDuration;
    }

    public List<DefaultProxy> getDefaultProxyList() {
        return defaultProxyList;
    }

    public List<String> getPreHeaterTaskList() {
        return preHeaterTaskList;
    }

    public static Context getInstance() {
        if (!hasInit) {
            synchronized (Context.class) {
                if (!hasInit) {
                    initEnv(null);
                }
            }
        }
        return instance;
    }

    public static void initEnv(ConfigBuilder builder) {
        if (hasInit) {
            return;
        }
        if (builder == null) {
            builder = ConfigBuilder.create();
            InputStream is = Context.class.getClassLoader().getResourceAsStream(ProxyConstant.configFileName);
            if (is != null) {
                Properties properties = new Properties();
                try {
                    properties.load(is);
                    builder.buildWithProperties(properties);
                } catch (IOException e) {
                    logger.error("error when load config file", e);
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }

        }
        instance = builder.build();
        hasInit = true;
    }

    public static class ConfigBuilder {
        private String resouceFace;

        private String proxyDomainStrategy;
        private String proxyDomainStrategyBlackList;
        private String proxyDomainStrategyWhiteList;

        private String offliner;

        private String feedBackDuration;

        private String avDumper;
        private String defaultAvDumpeFileName;
        private String defaultProxyList;
        private String preHeaterTaskList;

        public ConfigBuilder buildWithProperties(Properties properties) {
            if (properties == null) {
                return this;
            }
            resouceFace = properties.getProperty(ProxyConstant.RESOURCE_FACADE, ProxyConstant.DEFAULT_RESOURCE_FACADE);
            proxyDomainStrategy = properties.getProperty(ProxyConstant.PROXY_DOMAIN_STRATEGY,
                    ProxyConstant.DEFAULT_DOMAIN_STRATEGY);
            proxyDomainStrategyBlackList = properties.getProperty(ProxyConstant.BLACK_LIST_STRATEGY);
            proxyDomainStrategyWhiteList = properties.getProperty(ProxyConstant.WHITE_LIST_STRATEGY);

            feedBackDuration = properties.getProperty(ProxyConstant.FEEDBACK_DURATION);

            avDumper = properties.getProperty(ProxyConstant.PROXY_SERIALIZER);
            defaultAvDumpeFileName = properties.getProperty(ProxyConstant.DEFAULT_PROXY_SERALIZER_FILE);
            defaultProxyList = properties.getProperty(ProxyConstant.DEFAULT_PROXY_LIST);
            preHeaterTaskList = properties.getProperty(ProxyConstant.PREHEATER_TASK_LIST);
            return this;
        }

        public static ConfigBuilder create() {
            return new ConfigBuilder();
        }

        public ConfigBuilder setResouceFace(String resouceFace) {
            this.resouceFace = resouceFace;
            return this;
        }

        public ConfigBuilder setAvDumper(String avDumper) {
            this.avDumper = avDumper;
            return this;
        }

        public ConfigBuilder setDefaultAvDumpeFileName(String defaultAvDumpeFileName) {
            this.defaultAvDumpeFileName = defaultAvDumpeFileName;
            return this;
        }

        public ConfigBuilder setFeedBackDuration(String feedBackDuration) {
            this.feedBackDuration = feedBackDuration;
            return this;
        }

        public ConfigBuilder setOffliner(String offliner) {
            this.offliner = offliner;
            return this;
        }

        public ConfigBuilder setProxyDomainStrategy(String proxyDomainStrategy) {
            this.proxyDomainStrategy = proxyDomainStrategy;
            return this;
        }

        /**
         * 设置默认的代理,如果当前代理池还拿不到代理,则尝试使用默认,默认代理应该为一个转发服务器
         * 
         * @param defaultProxyList ip:port
         * @return configBuilder
         */
        public ConfigBuilder setDefaultProxyList(String defaultProxyList) {
            this.defaultProxyList = defaultProxyList;
            return this;
        }

        public ConfigBuilder setProxyDomainStrategyBlackList(String proxyDomainStrategyBlackList) {
            this.proxyDomainStrategyBlackList = proxyDomainStrategyBlackList;
            return this;
        }

        public ConfigBuilder setProxyDomainStrategyWhiteList(String proxyDomainStrategyWhiteList) {
            this.proxyDomainStrategyWhiteList = proxyDomainStrategyWhiteList;
            return this;
        }

        public Context build() {
            Context context = new Context();
            // resouceFace
            context.resourceFacade = StringUtils.isEmpty(this.resouceFace) ? ProxyConstant.DEFAULT_RESOURCE_FACADE
                    : this.resouceFace;

            // domainStrategy
            if (StringUtils.isEmpty(proxyDomainStrategy)) {
                proxyDomainStrategy = ProxyConstant.DEFAULT_DOMAIN_STRATEGY;
            }
            switch (proxyDomainStrategy) {
            case "WHITE_LIST":
                WhiteListProxyStrategy whiteListProxyStrategy = new WhiteListProxyStrategy();
                if (!StringUtils.isEmpty(proxyDomainStrategyWhiteList)) {
                    for (String domain : Splitter.on(",").omitEmptyStrings().trimResults()
                            .split(proxyDomainStrategyWhiteList)) {
                        whiteListProxyStrategy.addWhiteHost(domain);
                    }
                }
                context.needProxyStrategy = whiteListProxyStrategy;
                break;
            case "BLACK_LIST":
                BlackListProxyStrategy blackListProxyStrategy = new BlackListProxyStrategy();
                if (!StringUtils.isEmpty(proxyDomainStrategyBlackList)) {
                    for (String domain : Splitter.on(",").omitEmptyStrings().trimResults()
                            .split(proxyDomainStrategyBlackList)) {
                        blackListProxyStrategy.add2BlackList(domain);
                    }
                }
                context.needProxyStrategy = blackListProxyStrategy;
                break;
            default:
                context.needProxyStrategy = ObjectFactory.newInstance(proxyDomainStrategy);
            }

            // offliner
            if (this.offliner == null) {
                offliner = "com.virjar.dungproxy.client.ippool.strategy.offline.DefaultOffliner";
            }
            context.offliner = ObjectFactory.newInstance(offliner);

            if (this.feedBackDuration == null) {
                feedBackDuration = "120000";
            }
            context.feedBackDuration = NumberUtils.toInt(feedBackDuration, 120000);

            // avDumper
            if (this.avDumper == null) {
                this.avDumper = ProxyConstant.DEFAULT_PROXY_SERIALIZER;
            }
            if (this.avDumper.equals(ProxyConstant.DEFAULT_PROXY_SERIALIZER)) {
                if (defaultAvDumpeFileName == null) {
                    defaultAvDumpeFileName = ProxyConstant.DEFAULT_PROXY_SERALIZER_FILE_VALUE;
                }
                context.avProxyDumper = new JSONFileAvProxyDumper(defaultAvDumpeFileName);
            } else {
                context.avProxyDumper = ObjectFactory.newInstance(avDumper);
            }

            // default proxy
            resolveDefaultProxy(this.defaultProxyList, context);

            if (StringUtils.isNoneEmpty(this.preHeaterTaskList)) {
                context.preHeaterTaskList = Splitter.on(",").omitEmptyStrings().trimResults()
                        .splitToList(preHeaterTaskList);
            } else {
                context.preHeaterTaskList = Lists.newArrayList();
            }
            return context;
        }

        void resolveDefaultProxy(String proxyString, Context context) {
            if (StringUtils.isEmpty(proxyString)) {
                return;
            }
            try {
                Map<String, String> map = Splitter.on(",").omitEmptyStrings().trimResults().withKeyValueSeparator(":")
                        .split(proxyString);
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    int port = NumberUtils.toInt(entry.getValue(), 8081);
                    if (IpAvValidator.validateProxyConnect(new HttpHost(entry.getKey(), port))) {
                        DefaultProxy defaultProxy = new DefaultProxy();
                        defaultProxy.setIp(entry.getKey());
                        defaultProxy.setPort(port);
                        context.defaultProxyList.add(defaultProxy);
                    }
                }
                logger.info("统一代理服务,有效地址:{}", JSONObject.toJSONString(context.defaultProxyList));
            } catch (Exception e) {
                logger.warn("默认代理加载失败,不能识别的格式:{}", proxyString);
            }
        }
    }

    public PreHeater getPreHeater() {
        return preHeater;
    }
}
