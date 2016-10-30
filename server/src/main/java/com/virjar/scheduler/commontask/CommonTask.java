package com.virjar.scheduler.commontask;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 一些不需要随时执行的任务,如备份数据,同步地址,刷新域名IP池IP数据等<br/>
 * Created by virjar on 16/10/3.
 */
public abstract class CommonTask implements Callable<Object> {
    private long duration;

    private long lastRunTime;

    private AtomicBoolean isRunning = new AtomicBoolean(false);

    public CommonTask(long duration) {
        this.duration = duration;
        this.lastRunTime = 0;// System.currentTimeMillis() - new Random().nextInt(3600000);// 避免启动的时候所有任务同时启动
    }

    @Override
    public Object call() throws Exception {
        if (System.currentTimeMillis() - duration < lastRunTime) {
            return "时候未到";
        }
        if (!isRunning.compareAndSet(false, true)) {
            return "isRunning";
        }
        try {
            lastRunTime = System.currentTimeMillis();
            return execute();
        } finally {
            isRunning.set(false);
        }
    }

    public abstract Object execute();
}
