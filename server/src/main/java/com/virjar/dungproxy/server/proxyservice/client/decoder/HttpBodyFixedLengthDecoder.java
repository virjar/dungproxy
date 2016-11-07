package com.virjar.dungproxy.server.proxyservice.client.decoder;

import com.virjar.dungproxy.server.proxyservice.client.exception.HttpBodyDecodeFailException;
import com.virjar.dungproxy.server.proxyservice.client.listener.ResponseListener;
import io.netty.buffer.ByteBuf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpBodyFixedLengthDecoder extends HttpBodyDecoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpBodyFixedLengthDecoder.class);

    private long remaining;

    public HttpBodyFixedLengthDecoder(ResponseListener listener, ByteBuf initBuf, long contentLength, int headerIndex) {
        super(listener, State.FIXED_LENGTH, initBuf, headerIndex);
        this.remaining = contentLength;
    }

    @Override
    protected boolean decode(ByteBuf buf) throws HttpBodyDecodeFailException {
        try {
            int readableBytes = buf.readableBytes();
            remaining -= readableBytes;
            LOGGER.debug("Remaining bytes: {}", remaining);
            if (remaining <= 0) {
                LOGGER.debug("Decode finished. State [{}]", getState());
                if (remaining < 0) {
                    LOGGER.info("Actual data length is longer than expected, remaining {}", remaining);
                }
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new HttpBodyDecodeFailException(e);
        }
    }
}
