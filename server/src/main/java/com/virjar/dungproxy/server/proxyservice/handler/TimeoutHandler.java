package com.virjar.dungproxy.server.proxyservice.handler;

import com.virjar.dungproxy.server.proxyservice.common.util.NetworkUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Description: TimeoutHandler
 *
 * @author lingtong.fu
 * @version 2016-10-18 16:53
 */
@ChannelHandler.Sharable
public class TimeoutHandler extends ChannelDuplexHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(TimeoutHandler.class);

    public static TimeoutHandler instance = new TimeoutHandler();

    private TimeoutHandler() {
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            LOGGER.info("Channel [{}] Timeout", ctx.channel());
            NetworkUtil.closeChannel(ctx.channel());
        }
        super.userEventTriggered(ctx, evt);
    }
}
