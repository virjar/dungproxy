package com.virjar.dungproxy.server.proxyservice.client.listener;

import com.virjar.dungproxy.server.proxyservice.common.util.NetworkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractResponseListener implements ResponseListener {

    private AtomicBoolean isCompleted = new AtomicBoolean(false);

    protected RequestExecutor executor;

    @Override
    public void setRequestExecutor(RequestExecutor executor) {
        this.executor = executor;
    }

    /**
     * {@inheritDoc}
     * 如果请求已被标记为完成, 则直接返回 ABORT
     */
    @Override
    public State onConnectCompleted(Result result) {
        if (!isCompleted()) {
            return doOnConnectCompleted(result);
        }
        return State.ABORT;
    }

    /**
     * {@inheritDoc}
     * 如果请求已被标记为完成, 则忽略
     */
    @Override
    public boolean onHeaderReceived(HttpResponse httpMessage) {
        if (!isCompleted()) {
            return doOnHeaderReceived(httpMessage);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 如果请求已完成, 则释放消息, 否则回调 {@code doOnDataReceived()}
     */
    @Override
    public void onDataReceived(ByteBuf data) {
        if (!isCompleted()) {
            doOnDataReceived(data);
        } else {
            NetworkUtil.releaseMsgCompletely(data);
        }
    }

    /**
     * {@inheritDoc}
     * 如果是最后一次 flush, 则将 listener 状态标记为完成
     * 同时调用 {@code finishRequest()}
     */
    @Override
    public void onDataFlush(ChannelHandlerContext serverContext, boolean isLast) {
        if (isCompleted.compareAndSet(false, isLast)) {
            if (isLast && executor != null) {
                executor.finishRequest();
            }
            doOnDataFlush(serverContext, isLast);
        }
    }

    /**
     * {@inheritDoc}
     * 此方法会将 listener 标记为完成
     */
    @Override
    public void onConnectionPoolIsFull() {
        if (isCompleted.compareAndSet(false, true)) {
            executor.cancel(false);
            doOnConnectionPoolIsFull();
        }
    }

    /**
     * {@inheritDoc}
     * 此方法会将 listener 标记为完成
     */
    @Override
    public void onRequestTimeout() {
        if (isCompleted.compareAndSet(false, true)) {
            executor.cancel(false);
            doOnRequestTimeout();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onHandshakeSuccess() {
        doOnHandshakeSuccess();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRequestSent() {
        doOnRequestSent();
    }

    /**
     * {@inheritDoc}
     * 此方法会将 listener 标记为完成
     */
    @Override
    public void onThrowable(String errorTrace, Throwable cause) {
        if (isCompleted.compareAndSet(false, true)) {
            executor.cancel(false);
            doOnThrowable(errorTrace, cause);
        }
    }

    @Override
    public boolean isCompleted() {
        return isCompleted.get();
    }

    /**
     * 更新请求是否完成信息
     * @param expect 期待的原值
     * @param update 更新值
     * @return 更新结果
     */
    public boolean compareAndSetCompleted(boolean expect, boolean update) {
        return isCompleted.compareAndSet(expect, update);
    }

    protected abstract void doOnHandshakeSuccess();

    protected abstract void doOnRequestSent();

    protected abstract State doOnConnectCompleted(Result result);

    protected abstract boolean doOnHeaderReceived(HttpResponse httpMessage);

    protected abstract void doOnDataReceived(ByteBuf body);

    protected abstract void doOnDataFlush(ChannelHandlerContext serverContext, boolean isLast);

    protected abstract void doOnConnectionPoolIsFull();

    protected abstract void doOnRequestTimeout();

    protected abstract void doOnThrowable(String errorTrace, Throwable cause);
}