package com.virjar.ipproxy.ippool.config;

import org.apache.commons.lang.StringUtils;

import com.virjar.ipproxy.ippool.strategy.Importer;
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

    public static class ConfigBuilder {
        private String importer;

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
