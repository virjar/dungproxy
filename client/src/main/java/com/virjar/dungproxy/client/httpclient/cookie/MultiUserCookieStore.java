package com.virjar.dungproxy.client.httpclient.cookie;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;

import com.google.common.collect.Maps;

/**
 * Created by virjar on 17/2/11.<br/>
 * 打破了java基本原则,实际上我们已经不在走定义的方法了,这个类慎用,在非dungproxy外部使用将不会保证不会出现问题<br/>
 * 通过这个支持多用户的cookie维护,这让一个httpclient实例本身就会支持多个用户的cookie空间隔离。
 */
public class MultiUserCookieStore implements CookieStore {
    private static final String DEFAULT_USER = "DUNGPROXY_DEFAULT_USER";
    private ConcurrentMap<Object, CookieStore> cookieStores = Maps.newConcurrentMap();

    private CookieStoreGenerator cookieStoreGenerator;

    public MultiUserCookieStore() {
        this(null);
    }

    public MultiUserCookieStore(CookieStoreGenerator cookieStoreGenerator) {
        if (cookieStoreGenerator == null) {
            cookieStoreGenerator = new CookieStoreGenerator() {
                @Override
                public CookieStore generate() {
                    return new BasicCookieStore();
                }
            };
        }
        this.cookieStoreGenerator = cookieStoreGenerator;
    }

    @Override
    public void addCookie(Cookie cookie) {
        addCookie(cookie, null);
    }

    @Override
    public List<Cookie> getCookies() {
        return getCookies(null);
    }

    @Override
    public boolean clearExpired(Date date) {
        return clearExpired(date, null);
    }

    @Override
    public void clear() {
        clear(null);
    }

    public void addCookie(Cookie cookie, Object user) {
        createOrGetCookieStore(user).addCookie(cookie);
    }

    public List<Cookie> getCookies(Object user) {
        return createOrGetCookieStore(user).getCookies();
    }

    public boolean clearExpired(Date date, Object user) {
        return createOrGetCookieStore(user).clearExpired(date);
    }

    public void clear(Object user) {
        createOrGetCookieStore(user).clear();
    }

    public void clearAllUser() {
        for (CookieStore cookieStore : cookieStores.values()) {
            cookieStore.clear();
        }
    }

    private CookieStore createOrGetCookieStore(Object user) {
        if (user == null) {
            user = DEFAULT_USER;
        }
        CookieStore cookieStore = cookieStores.get(user);
        if (cookieStore == null) {
            synchronized (MultiUserCookieStore.class) {
                cookieStore = cookieStores.get(user);
                if (cookieStore == null) {
                    cookieStores.put(user, cookieStoreGenerator.generate());
                    cookieStore = cookieStores.get(user);
                }
            }

        }
        return cookieStore;
    }

}
