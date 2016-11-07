package com.virjar.dungproxy.server.proxyservice.client.listener;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponse;

public interface ResponseListener {

    class Result {
        private boolean success;
        private Throwable cause;
        private Object attr;

        public Result(boolean success, Throwable cause) {
            this.success = success;
            this.cause = cause;
        }

        public boolean isSuccess() {
            return success;
        }

        public Throwable getCause() {
            return cause;
        }

        public Object getAttr() {
            return attr;
        }

        public void setAttr(Object attr) {
            this.attr = attr;
        }
    }

    /**
     * 用于用户控制是否需要继续请求
     */
    public enum State {
        ABORT, CONTINUE
    }

    /**
     * 连接完成时回调
     * @param result 连接结果
     * @return 返回是否需要继续下面的流程
     */
    State onConnectCompleted(Result result);

    /**
     * SSL 握手完成时回调
     */
    void onHandshakeSuccess();

    /**
     * 请求发送完毕时回调
     */
    void onRequestSent();

    /**
     * 收到 Header 时回调
     * @param httpMessage Header
     * @return 是否重试
     */
    boolean onHeaderReceived(HttpResponse httpMessage);

    /**
     * 收到数据时回调
     * 这个方法应该负责释放 ByteBuf
     * @param data 收到的数据
     */
    void onDataReceived(ByteBuf data);

    /**
     * readCompleted 时回调
     * @param serverContext serverChannelContext
     * @param isLast 是否是最后一次 flush
     */
    void onDataFlush(ChannelHandlerContext serverContext, boolean isLast);

    /**
     * 连接池满时回调
     */
    void onConnectionPoolIsFull();

    /**
     * 请求超时时回调
     */
    void onRequestTimeout();

    /**
     * 出异常时回调
     * @param errorTrace 自定义错误信息
     * @param cause 异常信息
     */
    void onThrowable(String errorTrace, Throwable cause);

    void setRequestExecutor(RequestExecutor executor);

    /**
     * 请求是否完成
     * @return 请求是否完成
     */
    boolean isCompleted();
}
