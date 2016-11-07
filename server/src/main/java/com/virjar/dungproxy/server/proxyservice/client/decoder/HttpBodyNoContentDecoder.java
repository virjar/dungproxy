package com.virjar.dungproxy.server.proxyservice.client.decoder;

import com.virjar.dungproxy.server.proxyservice.client.listener.ResponseListener;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpBodyNoContentDecoder extends HttpBodyDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpBodyNoContentDecoder.class);

    public HttpBodyNoContentDecoder(ResponseListener listener, State state, ByteBuf header, int headerIndex) {
        super(listener, state, header, headerIndex);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        decodeFinished = true;
        initBuf.readerIndex(0);
        LOGGER.debug("Decode finished. State: [{}]", getState());
        listener.onDataReceived(initBuf);
        listener.onDataFlush(ctx, true);
    }

    @Override
    protected boolean decode(ByteBuf buf) {
        return false;
    }
}
