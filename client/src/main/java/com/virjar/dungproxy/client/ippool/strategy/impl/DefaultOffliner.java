package com.virjar.dungproxy.client.ippool.strategy.impl;

import com.virjar.dungproxy.client.ippool.strategy.Offline;
import com.virjar.dungproxy.client.model.Score;

/**
 * Created by virjar on 16/10/1.
 */
public class DefaultOffliner implements Offline {
    @Override
    public boolean needOffline(Score score) {
        return score.getReferCount().get() > 3 && score.getAvgScore() < 0.3D;// TODO 调整这个参数
    }
}
