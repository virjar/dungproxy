package com.virjar.client.concurrent;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Description: ManagedThreadPool
 *
 * @author lingtong.fu
 * @version 2016-09-04 13:02
 */
public class ManagedThreadPool extends ThreadPoolExecutor implements TimeCounter {
    private static final Logger log = LoggerFactory.getLogger(ManagedThreadPool.class);
    private final AtomicLong finishTime = new AtomicLong();
    private static final ThreadLocal<Long> local = new ThreadLocal();

    public ManagedThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, ManagedExecutors.defaultThreadFactory());
    }

    public ManagedThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, ManagedExecutors.defaultThreadFactory(), handler);
    }

    public ManagedThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public ManagedThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    public long getFinishTime() {
        return this.finishTime.get();
    }

    public void beforeExecute(Thread t, Runnable r) {
        local.set(Long.valueOf(System.currentTimeMillis()));
        super.beforeExecute(t, r);

        try {
            ThreadRecycles.init();
        } catch (RuntimeException var4) {
            log.warn("ThreadRecycles.init error", var4);
        }

    }

    protected void afterExecute(Runnable r, Throwable ex) {
        try {
            long e = ((Long)local.get()).longValue();
            local.remove();
            this.finishTime.addAndGet(System.currentTimeMillis() - e);
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

        if(ex != null) {
            log.warn("在线程池中捕获到未知异常:" + ex, ex);
        }

    }


    public static void main(String[] args) throws InterruptedException {
        ManagedThreadPool tpe = new ManagedThreadPool(0, 2147483647, 60L, TimeUnit.SECONDS, new SynchronousQueue());
        tpe.execute(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException var2) {
                    var2.printStackTrace();
                }

            }
        });
        Thread.sleep(1000L);
        Iterator i$ = Thread.getAllStackTraces().keySet().iterator();

        while(i$.hasNext()) {
            Thread t = (Thread)i$.next();
            System.out.println(t);
        }

        tpe.shutdown();
    }
}
