package com.virjar.dungproxy.server.proxyservice.client.decoder;

import com.virjar.dungproxy.server.proxyservice.client.listener.ResponseListener;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

public class BadMessageHandler extends ChannelDuplexHandler {

    private ResponseListener listener;

    private Throwable cause;

    public BadMessageHandler(ResponseListener listener, Throwable cause) {
        this.listener = listener;
        this.cause = cause;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        listener.onThrowable(this.getClass().getName() + " handlerAdded", cause);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        listener.onThrowable(this.getClass().getName() + " exceptionCaught", cause);
    }
}
