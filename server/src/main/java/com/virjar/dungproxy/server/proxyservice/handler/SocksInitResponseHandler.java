package com.virjar.dungproxy.server.proxyservice.handler;

import com.virjar.dungproxy.server.proxyservice.client.exception.EmptyException;
import com.virjar.dungproxy.server.proxyservice.client.listener.DefaultRequestExecutor;
import com.virjar.dungproxy.server.proxyservice.client.listener.ResponseListener;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.socks.SocksAddressType;
import io.netty.handler.codec.socks.SocksAuthScheme;
import io.netty.handler.codec.socks.SocksCmdRequest;
import io.netty.handler.codec.socks.SocksCmdResponseDecoder;
import io.netty.handler.codec.socks.SocksCmdType;
import io.netty.handler.codec.socks.SocksInitResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class SocksInitResponseHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(SocksInitResponseHandler.class);
    private static final EmptyException MESSAGE_TYPE_ERROR = new EmptyException("收到非 init 消息");
    private static final EmptyException AUTH_METHOD_EXCEPTION = new EmptyException("Socks 代理不支持无验证");

    private final ResponseListener listener;
    private final FullHttpRequest request;
    private final DefaultRequestExecutor executor;

    public SocksInitResponseHandler(ResponseListener listener, FullHttpRequest request, DefaultRequestExecutor executor) {
        this.listener = listener;
        this.request = request;
        this.executor = executor;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof SocksInitResponse) {
            SocksInitResponse response = (SocksInitResponse) msg;
            if (response.authScheme() != SocksAuthScheme.NO_AUTH) {
                logger.info("代理不支持无验证 socks. 支持的方式是 {}", response.authScheme().name());
                listener.onConnectCompleted(new ResponseListener.Result(false, AUTH_METHOD_EXCEPTION));
            } else {
                SocksAddressType addressType = SocksAddressType.IPv4;
                URI uri = URI.create(request.uri());
                if (!isIp(uri)) addressType = SocksAddressType.DOMAIN;
                int port = uri.getPort();
                if (port == -1) {
                    port = uri.getScheme().equalsIgnoreCase("http") ? 80 : 443;
                }
                ctx.channel().writeAndFlush(new SocksCmdRequest(SocksCmdType.CONNECT, addressType, uri.getHost(), port))
                        .addListener(new ChannelFutureListener() {
                            @Override
                            public void operationComplete(ChannelFuture future) throws Exception {
                                future.channel().pipeline()
                                        .addLast(new SocksCmdResponseDecoder())
                                        .addLast(new SocksCmdResponseHandler(listener, executor))
                                        .remove(SocksInitResponseHandler.this);
                            }
                        });
            }
        } else {
            listener.onThrowable(SocksInitResponseHandler.class.getName(), MESSAGE_TYPE_ERROR);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        listener.onThrowable(SocksInitResponseHandler.class.getName(), cause);
    }

    private boolean isIp(URI uri) {
        String host = uri.getHost();
        for (char c : host.toCharArray()) {
            if (!Character.isDigit(c) && c != '.') {
                return false;
            }
        }
        return true;
    }
}
