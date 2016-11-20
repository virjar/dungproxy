package com.virjar.dungproxy.client.ippool.strategy.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.virjar.dungproxy.client.ippool.strategy.Scoring;
import com.virjar.dungproxy.client.model.Score;

/**
 * Created by virjar on 16/11/16.
 */
public class DefaultScoring implements Scoring {
    private static final Logger logger = LoggerFactory.getLogger(DefaultScoring.class);

    @Override
    public double newAvgScore(Score score, int factory, boolean isSuccess) {
       // logger.info("old Score:{}  factory:{} isSuccess:{}", score.getAvgScore(), factory,isSuccess);
        if (score.getAvgScore() == 0D && isSuccess) {
            return 1D;
        }
        double newScore = isSuccess ? 1D : 0D;
        double ret = (score.getAvgScore() * (factory - 1) + newScore) / factory;
        //logger.info("newScore :{}", ret);
        return ret;
    }
}
