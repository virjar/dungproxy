package com.virjar.dungproxy.server.proxyservice.common;

import com.google.common.collect.Sets;

import java.util.Set;
/**
 * Description: ServerConstants
 *
 * @author lingtong.fu
 * @version 2016-10-18 16:21
 */
public class Constants {

    public static final String UNKNOWN = "unknown";

    public static final String USE_HTTPS_KEY = "Use-Https";
    public static final String CUSTOM_USER_AGENT_KEY = "Cus-User-Agent";
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String X_REAL_IP = "X-Real-IP";

    public static final Set<String> PROXY_HEADER_SET = Sets.newHashSet();

    static {
        PROXY_HEADER_SET.add(CUSTOM_USER_AGENT_KEY);
        PROXY_HEADER_SET.add(USE_HTTPS_KEY);
        PROXY_HEADER_SET.add(X_FORWARDED_FOR);
        PROXY_HEADER_SET.add(X_REAL_IP);
    }
}
