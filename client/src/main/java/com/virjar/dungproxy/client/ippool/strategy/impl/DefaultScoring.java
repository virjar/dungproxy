package com.virjar.dungproxy.client.ippool.strategy.impl;

import com.virjar.dungproxy.client.ippool.strategy.Scoring;
import com.virjar.dungproxy.client.model.Score;

/**
 * Created by virjar on 16/11/16.
 */
public class DefaultScoring implements Scoring {
    @Override
    public double newAvgScore(Score score, int factory, boolean isSuccess) {
        if (score.getAvgScore() == 0L && isSuccess) {
            return 1L;
        }
        long newScore = isSuccess ? 1L : 0L;
        return (score.getAvgScore() * (factory - 1) + newScore) / factory;
    }
}
