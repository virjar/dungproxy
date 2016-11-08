package com.virjar.dungproxy.server.proxyservice.common;

import io.netty.util.AttributeKey;

import static io.netty.util.AttributeKey.valueOf;

/**
 * Description: AttributeKeys
 *
 * @author lingtong.fu
 * @version 2016-11-07 16:07
 */
public class AttributeKeys {

    public static final AttributeKey<Integer> REQUEST_TIMEOUT = valueOf("requestTimeout");

    public static final AttributeKey<String> DOMAIN = valueOf("domain");
}
