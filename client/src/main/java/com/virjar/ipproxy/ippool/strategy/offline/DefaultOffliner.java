package com.virjar.ipproxy.ippool.strategy.offline;

/**
 * Created by virjar on 16/10/1.
 */
public class DefaultOffliner implements Offline {
    @Override
    public boolean needOffline(int totalUse, int failedTimes) {
        // TODO 这里需要可配,当前版本硬编码
        return totalUse > 10 && (failedTimes * 100 / totalUse) < 40;
    }
}
