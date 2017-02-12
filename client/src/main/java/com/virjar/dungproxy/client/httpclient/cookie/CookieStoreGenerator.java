package com.virjar.dungproxy.client.httpclient.cookie;

import org.apache.http.client.CookieStore;

/**
 * Created by virjar on 17/2/11.
 */
public interface CookieStoreGenerator {
    CookieStore generate(String user);
}
