package com.virjar.dungproxy.webmagic7;

import us.codecraft.webmagic.Page;

/**
 * Created by virjar on 17/6/2.
 */
public interface OfflineStrategy {
    boolean needOfflineProxy(Page page);

    class NotOfflineStrategy implements OfflineStrategy {

        @Override
        public boolean needOfflineProxy(Page page) {
            return false;
        }
    }
}
