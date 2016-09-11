package com.virjar;

import org.junit.Test;

import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Description: scheduleTest
 *
 * @author lingtong.fu
 * @version 2016-09-11 17:55
 */
public class scheduleTest {
    public static void main(String[] args) {
        final TimerTask task1 = new TimerTask() {
            @Override
            public void run() {
                System.out.println("task1 invoked!");
            }
        };
        final TimerTask task2 = new TimerTask() {
            @Override
            public void run() {
                System.out.println("task2 invoked!");
            }
        };
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);
        pool.schedule(task1, 100, TimeUnit.MILLISECONDS);
        pool.scheduleAtFixedRate(task2, 0 , 1000, TimeUnit.MILLISECONDS);
    }
}
