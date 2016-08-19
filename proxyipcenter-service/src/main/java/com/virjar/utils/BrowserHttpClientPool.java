package com.virjar.utils;

import org.apache.commons.pool2.BaseKeyedPooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * httpclient资源池 Created by virjar on 16/8/19.
 */
public class BrowserHttpClientPool {
    private static BrowserHttpClientPool instance = new BrowserHttpClientPool();
    private static final Logger logger = LoggerFactory.getLogger(BrowserHttpClientPool.class);

    public static BrowserHttpClientPool getInstance() {
        return instance;
    }

    private GenericKeyedObjectPool<UserKey, BrowserHttpClient> pool = null;
    private GenericKeyedObjectPool<UserKey, BrowserHttpClient> noKeyPool = null;

    private GenericKeyedObjectPoolConfig setupPool(boolean nokeyPool) {
        GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
        config.setMaxTotal(500); // 整个池最大值
        if(nokeyPool){
            config.setMaxTotalPerKey(500);
        }else {
            config.setMaxTotalPerKey(5); // 每个key的最大
        }
        config.setBlockWhenExhausted(true);
        config.setMinIdlePerKey(0);
        config.setMaxWaitMillis(-1); // 获取不到永远等待
        config.setNumTestsPerEvictionRun(Integer.MAX_VALUE); // always test all idle objects
        config.setTestOnBorrow(true);
        config.setTestOnReturn(false);
        config.setTestWhileIdle(false);
        config.setTimeBetweenEvictionRunsMillis(1 * 60000L); // -1不启动。默认1min一次
        config.setMinEvictableIdleTimeMillis(10 * 60000L); // 可发呆的时间,10mins
        config.setTestWhileIdle(false); // 发呆过长移除的时候是否test一下先
        return config;
    }

    private BrowserHttpClientPool() {
        pool = new GenericKeyedObjectPool<>(new BrowserHttpClientFactory(), setupPool(false));
        noKeyPool = new GenericKeyedObjectPool<>(new BrowserHttpClientFactory(), setupPool(true));
    }

    private UserKey commUserKey = new UserKey("common_topic", "common_user");

    public BrowserHttpClient borrow() {
        try {
            return noKeyPool.borrowObject(commUserKey);
        } catch (Exception e) {
            logger.error("can not borrow httpClient", e);
            return null;
        }
    }

    public BrowserHttpClient borrow(UserKey userKey) {
        try {
            return pool.borrowObject(userKey);
        } catch (Exception e) {
            logger.error("can not borrow httpClient", e);
            return null;
        }
    }

    /**
     * 在finally中执行
     * 
     * @param userKey 可以为null
     * @param browserHttpClient 不可以为null
     */
    public void returnBack(UserKey userKey, BrowserHttpClient browserHttpClient) {
        pool.returnObject(userKey, browserHttpClient);
    }

    public void returnBack(BrowserHttpClient browserHttpClient) {
        noKeyPool.returnObject(commUserKey,browserHttpClient);
    }

    /**
     * 用户是逻辑概念,代表某一个系统的某一个操作者,他是实现业务相互独立的保证
     */
    public class UserKey {
        private String topic;// 主题,表示业务系统
        private String user;// 用户,

        public UserKey(String topic, String user) {
            this.topic = topic;
            this.user = user;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            UserKey userKey = (UserKey) o;

            if (topic != null ? !topic.equals(userKey.topic) : userKey.topic != null)
                return false;
            return user != null ? user.equals(userKey.user) : userKey.user == null;

        }

        @Override
        public int hashCode() {
            int result = topic != null ? topic.hashCode() : 0;
            result = 31 * result + (user != null ? user.hashCode() : 0);
            return result;
        }
    }

    private class BrowserHttpClientFactory extends BaseKeyedPooledObjectFactory<UserKey, BrowserHttpClient> {

        @Override
        public BrowserHttpClient create(UserKey key) throws Exception {
            return new BrowserHttpClient();
        }

        @Override
        public PooledObject<BrowserHttpClient> wrap(BrowserHttpClient value) {
            return new DefaultPooledObject<>(value);
        }
    }
}
