package com.virjar.concurrent;

/**
 * Description: ManagedExecutors
 *
 * @author lingtong.fu
 * @version 2016-09-04 12:40
 */
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ManagedExecutors {
    private static final ThreadFactory cachedFactory = new NamedThreadFactory("shared-pool");
    private static final ThreadFactory scheduleFactory = new NamedThreadFactory("shared-sched");
    private static final ManagedThreadPool executor = (ManagedThreadPool)newCachedThreadPool();
    private static final ManagedScheduledThreadPool mstp = (ManagedScheduledThreadPool)newScheduledThreadPool(10);
    private static final ThreadFactory NON_FAC;

    private ManagedExecutors() {
    }

    public static ManagedThreadPool getExecutor() {
        return executor;
    }

    public static ManagedScheduledThreadPool getScheduleExecutor() {
        return mstp;
    }

    public static ExecutorService newFixedThreadPool(int nThreads) {
        return new ManagedThreadPool(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue());
    }

    public static ExecutorService newFixedThreadPool(int nThreads, String threadName) {
        return new ManagedThreadPool(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(), new NamedThreadFactory(threadName));
    }

    public static ExecutorService newFixedThreadPool(int nThreads, ThreadFactory threadFactory) {
        return new ManagedThreadPool(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(), threadFactory);
    }

    public static ExecutorService newSingleThreadExecutor() {
        return new ManagedExecutors.FinalizableDelegatedExecutorService(new ManagedThreadPool(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue()));
    }

    public static ExecutorService newSingleThreadExecutor(String threadName) {
        return new ManagedExecutors.FinalizableDelegatedExecutorService(new ManagedThreadPool(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(), new NamedThreadFactory(threadName)));
    }

    public static ExecutorService newSingleThreadExecutor(ThreadFactory threadFactory) {
        return new ManagedExecutors.FinalizableDelegatedExecutorService(new ManagedThreadPool(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue(), threadFactory));
    }

    public static ExecutorService newCachedThreadPool() {
        return new ManagedThreadPool(0, 2147483647, 60L, TimeUnit.SECONDS, new SynchronousQueue());
    }

    public static ExecutorService newCachedThreadPool(String threadName) {
        return new ManagedThreadPool(0, 2147483647, 60L, TimeUnit.SECONDS, new SynchronousQueue(), new NamedThreadFactory(threadName));
    }

    public static ExecutorService newCachedThreadPool(ThreadFactory threadFactory) {
        return new ManagedThreadPool(0, 2147483647, 60L, TimeUnit.SECONDS, new SynchronousQueue(), threadFactory);
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor() {
        return Executors.unconfigurableScheduledExecutorService(new ManagedScheduledThreadPool(1));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(String threadName) {
        return Executors.unconfigurableScheduledExecutorService(new ManagedScheduledThreadPool(1, new NamedThreadFactory(threadName)));
    }

    public static ScheduledExecutorService newSingleThreadScheduledExecutor(ThreadFactory threadFactory) {
        return Executors.unconfigurableScheduledExecutorService(new ManagedScheduledThreadPool(1, threadFactory));
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize) {
        return new ManagedScheduledThreadPool(corePoolSize);
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, String threadName) {
        return new ManagedScheduledThreadPool(corePoolSize, new NamedThreadFactory(threadName));
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize, ThreadFactory threadFactory) {
        return new ManagedScheduledThreadPool(corePoolSize, threadFactory);
    }

    public static ExecutorService unconfigurableExecutorService(ExecutorService executor) {
        return Executors.unconfigurableExecutorService(executor);
    }

    public static ScheduledExecutorService unconfigurableScheduledExecutorService(ScheduledExecutorService executor) {
        return Executors.unconfigurableScheduledExecutorService(executor);
    }

    public static ThreadFactory defaultThreadFactory() {
        String name = null;
        Throwable t = new Throwable();
        StackTraceElement[] arr$ = t.getStackTrace();
        int len$ = arr$.length;

        for(int i$ = 0; i$ < len$; ++i$) {
            StackTraceElement st = arr$[i$];
            String className = st.getClassName();
            if(!className.startsWith("con.virjar.concurrent")) {
                name = "anon-" + st.getClassName() + "." + st.getMethodName() + ":" + st.getLineNumber();
                return new NamedThreadFactory(name);
            }
        }

        return NON_FAC;
    }

    public static ThreadFactory privilegedThreadFactory() {
        return Executors.privilegedThreadFactory();
    }

    public static <T> Callable<T> callable(Runnable task, T result) {
        return Executors.callable(task, result);
    }

    public static Callable<Object> callable(Runnable task) {
        return Executors.callable(task);
    }

    public static Callable<Object> callable(PrivilegedAction<?> action) {
        return Executors.callable(action);
    }

    public static Callable<Object> callable(PrivilegedExceptionAction<?> action) {
        return Executors.callable(action);
    }

    public static <T> Callable<T> privilegedCallable(Callable<T> callable) {
        return Executors.privilegedCallable(callable);
    }

    public static <T> Callable<T> privilegedCallableUsingCurrentClassLoader(Callable<T> callable) {
        return Executors.privilegedCallableUsingCurrentClassLoader(callable);
    }

    static {
        executor.setThreadFactory(cachedFactory);
        mstp.setThreadFactory(scheduleFactory);
        NON_FAC = new NamedThreadFactory("anon-pool");
    }

    static class FinalizableDelegatedExecutorService extends ManagedExecutors.DelegatedExecutorService {
        FinalizableDelegatedExecutorService(ExecutorService executor) {
            super(executor);
        }

        protected void finalize() {
            super.shutdown();
        }
    }

    static class DelegatedExecutorService extends AbstractExecutorService {
        private final ExecutorService e;

        DelegatedExecutorService(ExecutorService executor) {
            this.e = executor;
        }

        public void execute(Runnable command) {
            this.e.execute(command);
        }

        public void shutdown() {
            this.e.shutdown();
        }

        public List<Runnable> shutdownNow() {
            return this.e.shutdownNow();
        }

        public boolean isShutdown() {
            return this.e.isShutdown();
        }

        public boolean isTerminated() {
            return this.e.isTerminated();
        }

        public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
            return this.e.awaitTermination(timeout, unit);
        }

        public Future<?> submit(Runnable task) {
            return this.e.submit(task);
        }

        public <T> Future<T> submit(Callable<T> task) {
            return this.e.submit(task);
        }

        public <T> Future<T> submit(Runnable task, T result) {
            return this.e.submit(task, result);
        }

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return this.e.invokeAll(tasks);
        }

        public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
            return this.e.invokeAll(tasks, timeout, unit);
        }

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return this.e.invokeAny(tasks);
        }

        public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return this.e.invokeAny(tasks, timeout, unit);
        }
    }
}
