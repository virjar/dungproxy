package com.virjar.client.concurrent;

/**
 * Description: ManagedScheduledThreadPool
 *
 * @author lingtong.fu
 * @version 2016-09-04 12:43
 */

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ManagedScheduledThreadPool extends ScheduledThreadPoolExecutor implements TimeCounter {
    private static final Logger log = LoggerFactory.getLogger(ManagedThreadPool.class);
    private final AtomicLong finishTime = new AtomicLong();
    private static final ThreadLocal<Long> local = new ThreadLocal();

    public ManagedScheduledThreadPool(int corePoolSize) {
        super(corePoolSize, ManagedExecutors.defaultThreadFactory());
    }

    public ManagedScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    public ManagedScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
    }

    public ManagedScheduledThreadPool(int corePoolSize, RejectedExecutionHandler handler) {
        super(corePoolSize, ManagedExecutors.defaultThreadFactory(), handler);
    }

    public long getFinishTime() {
        return this.finishTime.get();
    }

    public void beforeExecute(Thread t, Runnable r) {
        local.set(DateTime.now().getMillis());
        super.beforeExecute(t, r);

        try {
            ThreadRecycles.init();
        } catch (RuntimeException var4) {
            log.warn("ThreadRecycles.init error", var4);
        }

    }

    protected void afterExecute(Runnable r, Throwable ex) {
        try {
            long e = local.get();
            local.remove();
            this.finishTime.addAndGet(DateTime.now().getMillis() - e);
        } catch (Throwable var11) {
            var11.printStackTrace();
        }

        try {
            ThreadRecycles.release();
        } catch (Throwable var9) {
            log.warn("ThreadRecycles.release error", var9);
        } finally {
            super.afterExecute(r, ex);
        }

        if (ex != null) {
            log.warn("在线程池中捕获到未知异常:" + ex, ex);
        }

    }

}
