package com.virjar.client.proxyclient;

import com.virjar.client.http.HttpOption;

import java.util.Map;

/**
 * @author zhizhan.wu
 */
public class ProxyOption extends HttpOption {


    @Override
    public ProxyOption addHeader(String key, String value) {
        return (ProxyOption) super.addHeader(key, value);
    }

    public ProxyOption setProxy(String host, int port) {
        return (ProxyOption) super.setProxy(host, port);
    }

    public ProxyOption setTtl(long timeoutms) {
        return addHeader("ttl", String.valueOf(timeoutms));
    }

    public ProxyOption setUserAgent(String userAgent) {
        addHeader("Cus-User-Agent", "1");
        return addHeader("User-Agent", userAgent);
    }

    public ProxyOption setUseHttps(boolean isUse) {
        return addHeader("Use-Https", isUse ? "1" : "0");
    }

    public ProxyOption setOffline(boolean isOffline) {
        return addHeader("Offline", isOffline ? "1" : "0");
    }

    static ProxyOption getRealOption(HttpOption option) {
        ProxyOption realOption = new ProxyOption();

        if (option != null) {
            //设置header
            for (Map.Entry<String, String> entry : option.getHeaders().entrySet()) {
                realOption.addHeader(entry.getKey(), entry.getValue());
            }
        }

        realOption.setProxy("localhost", 8081);

        return realOption;


    }

}
