package com.virjar.proxyservice.handler;

import com.virjar.proxyservice.common.ProxyResponse;
import com.virjar.proxyservice.common.util.NetworkUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Description: 无状态的非法请求 handler
 *
 * @author lingtong.fu
 * @version 2016-10-18 17:57
 */
@ChannelHandler.Sharable
public class BadRequestHandler extends EndpointHandler {
    public static final BadRequestHandler instance = new BadRequestHandler();

    private BadRequestHandler() {
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            NetworkUtil.writeAndFlushAndClose(ctx.channel(), new ProxyResponse(new HttpResponseStatus(400, "PROXY SCHEMA ERROR")));
        } finally {
            NetworkUtil.releaseMsgCompletely(msg);
        }
    }
}
