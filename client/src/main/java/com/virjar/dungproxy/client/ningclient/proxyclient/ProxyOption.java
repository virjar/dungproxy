package com.virjar.dungproxy.client.ningclient.proxyclient;

import com.virjar.dungproxy.client.ningclient.http.HttpOption;
import com.virjar.dungproxy.client.util.PropertiesUtil;

import java.util.Map;

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
            for (Map.Entry<String, String> entry : option.getHeaders().entrySet()) {
                realOption.addHeader(entry.getKey(), entry.getValue());
            }
        }
        realOption.setProxy(
                PropertiesUtil.getProperty("proxyclient.defaultUrl"),
                Integer.parseInt(PropertiesUtil.getProperty("proxyclient.defaultPort"))
        );
        return realOption;
    }

}
