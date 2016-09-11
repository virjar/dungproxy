package com.virjar.ippool.schedule;

/**
 * Description: 预热器
 *
 * @author lingtong.fu
 * @version 2016-09-11 18:16
 */
public class Preheater {

    public static Preheater getInstance() {
        return InstanceHolder.instance;
    }

    private static class InstanceHolder {
        public static Preheater instance = new Preheater();
    }
}
