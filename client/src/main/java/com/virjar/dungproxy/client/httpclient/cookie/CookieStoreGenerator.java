package com.virjar.dungproxy.client.httpclient.cookie;

import org.apache.http.client.CookieStore;

/**
 * Created by virjar on 17/2/11.<br/>
 * 注意,自己定义的cookieStore需要保证线程安全
 */
public interface CookieStoreGenerator {
    CookieStore generate(String user);
}
