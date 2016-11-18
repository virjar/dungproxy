package com.virjar.dungproxy.client.ippool.strategy.impl;

import com.virjar.dungproxy.client.ippool.strategy.Scoring;

/**
 * Created by virjar on 16/11/16.
 */
public class DefaultScoring implements Scoring {
    @Override
    public double newAvgScore(double avgScore, int factory, long totalUsage, long failedTimes, boolean isSuccess) {
        if (avgScore == 0L && isSuccess) {
            return 1L;
        }
        long newScore = isSuccess ? 1L : 0L;
        return (avgScore * (factory - 1) + newScore) / factory;
    }
}
