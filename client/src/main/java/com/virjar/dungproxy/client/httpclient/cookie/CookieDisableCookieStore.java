package com.virjar.dungproxy.client.httpclient.cookie;

import java.util.Date;
import java.util.List;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;

import com.google.common.collect.Lists;

/**
 * Created by virjar on 17/3/18.<br/>
 * 如果用这个替换cookieStore,那么类似于禁用cookie的功能了
 */
public class CookieDisableCookieStore implements CookieStore {
    private List<Cookie> emptyCookieStore = Lists.newArrayList();

    @Override
    public void addCookie(Cookie cookie) {

    }

    @Override
    public List<Cookie> getCookies() {
        return emptyCookieStore;
    }

    @Override
    public boolean clearExpired(Date date) {
        return true;
    }

    @Override
    public void clear() {

    }
}
