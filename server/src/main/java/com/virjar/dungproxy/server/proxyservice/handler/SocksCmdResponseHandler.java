package com.virjar.dungproxy.server.proxyservice.handler;

import com.virjar.dungproxy.server.proxyservice.client.exception.EmptyException;
import com.virjar.dungproxy.server.proxyservice.client.listener.DefaultRequestExecutor;
import com.virjar.dungproxy.server.proxyservice.client.listener.ResponseListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.socks.SocksCmdResponse;
import io.netty.handler.codec.socks.SocksCmdStatus;
import io.netty.handler.codec.socks.SocksMessageEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SocksCmdResponseHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SocksInitResponseHandler.class);
    private static final EmptyException MESSAGE_TYPE_ERROR = new EmptyException("收到非 cmd 消息");
    private static final EmptyException CONNECTION_ERROR = new EmptyException("socks 代理连接失败");

    private ResponseListener listener;
    private DefaultRequestExecutor executor;

    public SocksCmdResponseHandler(ResponseListener listener, DefaultRequestExecutor executor) {
        this.listener = listener;
        this.executor = executor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof SocksCmdResponse) {
            SocksCmdResponse response = (SocksCmdResponse) msg;
            if (response.cmdStatus() != SocksCmdStatus.SUCCESS) {
                logger.error("socks 代理链接失败 {}", response.cmdStatus());
                listener.onConnectCompleted(new ResponseListener.Result(false, CONNECTION_ERROR));
            } else {
                ChannelPipeline pipeline = ctx.channel().pipeline();
                pipeline.remove(SocksMessageEncoder.class);
                pipeline.remove(SocksCmdResponseHandler.class);
                executor.continueRequestAfterSocksInit(ctx.channel());
            }
        } else {
            listener.onThrowable(this.getClass().getName(), MESSAGE_TYPE_ERROR);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        listener.onThrowable(this.getClass().getName(), cause);
    }
}
