package com.virjar.ipproxy.ippool.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.virjar.ipproxy.ippool.strategy.serialization.JSONFileAvProxyDumper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Splitter;
import com.virjar.ipproxy.ippool.strategy.offline.Offline;
import com.virjar.ipproxy.ippool.strategy.proxydomain.BlackListProxyStrategy;
import com.virjar.ipproxy.ippool.strategy.proxydomain.ProxyDomainStrategy;
import com.virjar.ipproxy.ippool.strategy.proxydomain.WhiteListProxyStrategy;
import com.virjar.ipproxy.ippool.strategy.serialization.AvProxyDumper;

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

    private Context() {
    }

    public AvProxyDumper getAvProxyDumper() {
        return avProxyDumper;
    }

    // 唯一持有的对象,存储策略
    private static Context instance;
    private static volatile boolean hasInit = false;

    public Offline getOffliner() {
        return offliner;
    }

    public int getFeedBackDuration() {
        return feedBackDuration;
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
            return this;
        }

        public static ConfigBuilder create() {
            return new ConfigBuilder();
        }

        public ConfigBuilder setResouceFace(String resouceFace) {
            this.resouceFace = resouceFace;
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
                offliner = "com.virjar.ipproxy.ippool.strategy.offline.DefaultOffliner";
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
            return context;
        }
    }

}
