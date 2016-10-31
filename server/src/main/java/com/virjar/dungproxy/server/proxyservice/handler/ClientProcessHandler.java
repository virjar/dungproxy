package com.virjar.dungproxy.server.proxyservice.handler;

/**
 * Description:
 *
 * ClientProcessHandler处理Server端Exception
 * 客户端 Exception 由 EndpointHandler 处理
 *
 * @author lingtong.fu
 * @version 2016-10-18 16:17
 */

import com.virjar.dungproxy.server.proxyservice.common.Constants;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ClientProcessHandler extends ChannelInboundHandlerAdapter {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        String exceptionMsg = cause == null ? Constants.UNKNOWN : cause.getClass().getName();
        if (ctx.pipeline().get(EndpointHandler.class) != null) {
            super.exceptionCaught(ctx, cause);
        } else {
            logger.warn("客户端 {} 出现异常, 将关闭客户端 channel", ctx.channel(), cause);
            ctx.channel().close();
        }
    }
}