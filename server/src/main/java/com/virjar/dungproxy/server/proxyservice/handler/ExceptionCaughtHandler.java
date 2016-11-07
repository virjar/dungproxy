package com.virjar.dungproxy.server.proxyservice.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Description: ServerConstants
 *
 * @author lingtong.fu
 * @version 2016-11-06 16:28
 */
public class ExceptionCaughtHandler extends ChannelInboundHandlerAdapter {

    private static Logger LOGGER = LoggerFactory.getLogger(ExceptionCaughtHandler.class);

    private AtomicBoolean ignoreException = new AtomicBoolean(true);

    public void setIgnoreException(boolean value) {
        ignoreException.set(value);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        if (ignoreException.get()) {
            LOGGER.debug("Ignore exception.", cause);
            return;
        }
        super.exceptionCaught(ctx, cause);
    }
}
