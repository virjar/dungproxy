package com.virjar.dungproxy.server.utils;

/**
 * Created by virjar on 16/8/14.
 */
public class ScoreUtil {
    public static volatile int maxAvailbelScore = 0;
    public static volatile int minAvailbelScore = 0;
    public static volatile int maxConnectionScore = 0;
    public static volatile int minConnectionScore = 0;

    /**
     * 根据当前分值估计资源所在的槽,注意这个估计只是大概的,实际可能有少许偏差
     * 
     * @param score 资源打分
     * @return
     */
    public static long calAvailableSlot(long score) {
        long frame = calcFrame(maxAvailbelScore, minAvailbelScore, score);
        return Math.abs(score) / frame;
    }

    /**
     * 根据当前分值估计资源所在的槽,注意这个估计只是大概的,实际可能有少许偏差
     *
     * @param score 资源打分
     * @return
     */
    public static long calConnectSlot(long score) {
        long frame = calcFrame(maxConnectionScore, minConnectionScore, score);
        return Math.abs(score) / frame;
    }

    private static long calcFrame(long max, long min, long score) {
        if (score > 0) {
            if (max < SysConfig.getInstance().getAvaliableSlotNumber()) {
                return 1;
            } else {
                return max / SysConfig.getInstance().getAvaliableSlotNumber();
            }
        } else {
            if (-min < SysConfig.getInstance().getAvaliableSlotNumber()) {
                return 1;
            } else {
                return -min / SysConfig.getInstance().getAvaliableSlotNumber();
            }
        }
    }
}
