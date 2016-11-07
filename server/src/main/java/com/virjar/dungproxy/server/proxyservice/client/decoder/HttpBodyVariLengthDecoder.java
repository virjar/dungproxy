package com.virjar.dungproxy.server.proxyservice.client.decoder;

import com.virjar.dungproxy.server.proxyservice.client.listener.ResponseListener;
import com.virjar.dungproxy.server.proxyservice.common.util.NetworkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpBodyVariLengthDecoder extends HttpBodyDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpBodyVariLengthDecoder.class);

    public HttpBodyVariLengthDecoder(ResponseListener listener, ByteBuf initBuf, int headerIndex) {
        super(listener, State.VARIABLE_LENGTH, initBuf, headerIndex);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (headerWrite.compareAndSet(false, true)) {
            NetworkUtil.releaseMsgCompletely(initBuf);
        }
        LOGGER.debug("Decode finished. State [{}]", getState());
        listener.onDataReceived(Unpooled.EMPTY_BUFFER);
        listener.onDataFlush(ctx, true);
    }

    @Override
    protected boolean decode(ByteBuf buf) {
        return false;
    }
}
