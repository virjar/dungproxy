package com.virjar.dungproxy.server.proxyservice.client.decoder;

import com.virjar.dungproxy.server.proxyservice.client.exception.HttpBodyDecodeFailException;
import com.virjar.dungproxy.server.proxyservice.client.listener.ResponseListener;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpConstants;


public class HttpBodyChunkDecoder extends HttpBodyDecoder {

    private static final String CR_LF = String.valueOf(new char[]{HttpConstants.CR, HttpConstants.LF});

    private static final String CR_LF_CR_LF = CR_LF + CR_LF;

    private int chunkSize = -1;

    private int remaining = -1; // Indicate how much data to be read next time

    private StringBuilder cumulation = new StringBuilder(8);

    public HttpBodyChunkDecoder(ResponseListener listener, ByteBuf initChunk, int headerIndex) {
        super(listener, State.READ_CHUNK_SIZE, initChunk, headerIndex);
    }

    @Override
    protected boolean decode(ByteBuf buf) throws HttpBodyDecodeFailException {
        try {
            loop:
            for (; ; ) {
                loop2:
                switch (getState()) {
                    case READ_CHUNK_SIZE: {
                        while (buf.isReadable()) {
                            char nextByte = (char) buf.readByte();
                            if (nextByte == HttpConstants.LF) {
                                // Finish reading
                                chunkSize = getChunkSize(cumulation.toString());
                                remaining = chunkSize + 2; // remaining = chunkSize + CR + LF
                                cumulation.setLength(0);
                                setState(State.READ_CHUNK_CONTENT);
                                break loop2;
                            } else if (nextByte != HttpConstants.CR) {
                                cumulation.append(nextByte);
                            }
                        }
                        break loop;
                    }
                    case READ_CHUNK_CONTENT: {
                        if (chunkSize == 0) {
                            setState(State.READ_CHUNK_FOOTER);
                            remaining = 0;
                            cumulation.setLength(0);
                            break;
                        } else {
                            if (buf.readableBytes() < remaining) {
                                remaining -= buf.readableBytes();
                                break loop;
                            } else if (buf.readableBytes() == remaining) {
                                setState(State.READ_CHUNK_SIZE);
                                remaining = 0;
                                chunkSize = 0;
                                break loop;
                            } else { // Skip chunk content
                                buf.readerIndex(buf.readerIndex() + remaining);
                                setState(State.READ_CHUNK_SIZE);
                                remaining = 0;
                                chunkSize = 0;
                                break;
                            }
                        }
                    }
                    case READ_CHUNK_FOOTER: {
                        while (buf.isReadable()) {
                            cumulation.append((char) buf.readByte());
                        }
                        if (cumulation.length() >= 2 && cumulation.subSequence(0, 2).equals(CR_LF)) {
                            return true;
                        } else if (cumulation.length() >= 4 && cumulation.subSequence(cumulation.length() - 4, cumulation.length()).equals(CR_LF_CR_LF)) {
                            return true;
                        }
                        break loop;
                        // Pending response
                    }
                }
            }
            return false;
        } catch (Exception e) {
            throw new HttpBodyDecodeFailException(e);
        }

    }

    private static int getChunkSize(String hex) {
        hex = hex.trim();
        for (int i = 0; i < hex.length(); i++) {
            char c = hex.charAt(i);
            if (c == ';' || Character.isWhitespace(c) || Character.isISOControl(c)) {
                hex = hex.substring(0, i);
                break;
            }
        }

        return Integer.parseInt(hex, 16);
    }

}
