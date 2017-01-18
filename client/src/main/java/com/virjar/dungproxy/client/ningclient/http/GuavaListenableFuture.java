package com.virjar.dungproxy.client.ningclient.http;

/**
 * Description: GuavaListenableFuture
 *
 * @author lingtong.fu
 * @version 2016-09-04 22:57
 */
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class GuavaListenableFuture<T> implements ListenableFuture<T> {

    private final com.ning.http.client.ListenableFuture<T> future;

    public GuavaListenableFuture(com.ning.http.client.ListenableFuture<T> future) {
        this.future = future;
    }

    @Override
    public void addListener(Runnable listener, Executor executor) {
        future.addListener(listener, executor);
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    @Override
    public boolean isCancelled() {
        return future.isCancelled();
    }

    @Override
    public boolean isDone() {
        return future.isDone();
    }

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return future.get();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return future.get(timeout, unit);
    }
}

