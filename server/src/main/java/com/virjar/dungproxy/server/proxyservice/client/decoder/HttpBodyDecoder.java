package com.virjar.dungproxy.server.proxyservice.client.decoder;

import com.virjar.dungproxy.server.proxyservice.client.exception.HttpBodyDecodeFailException;
import com.virjar.dungproxy.server.proxyservice.client.exception.ServerChannelInactiveException;
import com.virjar.dungproxy.server.proxyservice.client.listener.ResponseListener;
import com.virjar.dungproxy.server.proxyservice.common.util.NetworkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.concurrent.atomic.AtomicBoolean;

public abstract class HttpBodyDecoder extends ChannelInboundHandlerAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpBodyDecoder.class);

    public enum State {
        READ_CHUNK_SIZE,
        READ_CHUNK_CONTENT,
        READ_CHUNK_FOOTER,
        VARIABLE_LENGTH,
        FIXED_LENGTH,
        NO_CONTENT,
        BAD_MESSAGE
    }

    private State state;
    protected ResponseListener listener;
    protected ByteBuf initBuf;
    protected boolean decodeFinished;
    private int headerIndex;
    protected AtomicBoolean headerWrite = new AtomicBoolean(false);
    private boolean hasDataRemained = false;

    protected HttpBodyDecoder(final ResponseListener listener, State state, ByteBuf buf, int headerIndex) {
        this.listener = listener;
        this.state = state;
        this.initBuf = buf;
        this.headerIndex = headerIndex;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        try {
            initBuf.readerIndex(headerIndex);
            if (headerWrite.compareAndSet(false, true)) {
                writeMsg(initBuf);
            }
        } catch (Exception e) {
            listener.onThrowable(getClass().getName() + " handlerAdded", e);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, final Object msg) throws Exception {
        writeMsg((ByteBuf) msg);
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        if (hasDataRemained) {
            listener.onDataFlush(ctx, decodeFinished);
            hasDataRemained = false;
        }
    }

    public final void writeMsg(ByteBuf msg) throws Exception {
        try {
            final boolean result = decodeFinished = decode(msg);
            if (result) {
                LOGGER.debug("Decode finished. State: [{}]", state);
            }
            msg.readerIndex(0);
            listener.onDataReceived(msg);
            hasDataRemained = true;
        } catch (Exception e) {
            NetworkUtil.releaseMsg(msg);
            throw e;
        }
    }

    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }

    /**
     * Decode method
     *
     * @param buf buf to be decoded
     * @return <tt>true<tt/> if response has reached the end, otherwise <tt>false</tt>
     */
    protected abstract boolean decode(ByteBuf buf) throws HttpBodyDecodeFailException;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (headerWrite.compareAndSet(false, true)) {
            NetworkUtil.releaseMsgCompletely(initBuf);
        }
        LOGGER.debug("Exception occurred while decoding response body", cause);
        listener.onThrowable(getClass().getName() + " exceptionCaught", cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (headerWrite.compareAndSet(false, true)) {
            NetworkUtil.releaseMsgCompletely(initBuf);
        }
        if (!decodeFinished) {
            String msg = "Server channel inactive while decoding response body";
            LOGGER.info(msg);
            listener.onThrowable(msg, ServerChannelInactiveException.INSTANCE);
        }
        super.channelInactive(ctx);
    }
}
