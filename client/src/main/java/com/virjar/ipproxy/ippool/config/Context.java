package com.virjar.ipproxy.ippool.config;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;

import com.virjar.ipproxy.ippool.strategy.importer.Importer;
import com.virjar.ipproxy.ippool.strategy.proxydomain.ProxyDomainStrategy;

/**
 * client配置 Created by virjar on 16/9/30.
 */
public class Context {
    // IP资源引入器 一般不需要修改
    private Importer importer;

    // 代理网站过滤器,通过这个类确认哪些网站需要进行代理
    private ProxyDomainStrategy needProxyStrategy;

    private Context() {
    }

    // 唯一持有的对象,存储策略
    private static Context instance;
    private static volatile boolean hasInit = false;

    public static Context getInstance() {
        if (!hasInit) {
            initEnv(null);
        }
        return instance;
    }

    public static void initEnv(ConfigBuilder builder) {
        if (hasInit) {
            return;
        }
        if (builder == null) {
            builder = ConfigBuilder.create();
        }
        instance = builder.build();
        hasInit = true;
    }

    public static class ConfigBuilder {
        private String importer;

        private String proxyDomainStrategy;

        public ConfigBuilder buildWithProperties(Properties properties) {
            if (properties == null) {
                return this;
            }
            importer = properties.getProperty(ProxyConstant.IMPORTER);
            return this;
        }

        public static ConfigBuilder create() {
            return new ConfigBuilder();
        }

        public ConfigBuilder setImporter(String importer) {
            this.importer = importer;
            return this;
        }

        public Context build() {
            Context context = new Context();
            if (StringUtils.isNotBlank(importer)) {
                context.importer = ObjectFactory.newInstance(importer);
            }
            return context;
        }
    }
}
