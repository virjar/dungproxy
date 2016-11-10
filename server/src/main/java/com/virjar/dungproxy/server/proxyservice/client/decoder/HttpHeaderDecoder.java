package com.virjar.dungproxy.server.proxyservice.client.decoder;

import com.virjar.dungproxy.server.entity.Proxy;
import com.virjar.dungproxy.server.proxyservice.client.exception.ServerChannelInactiveException;
import com.virjar.dungproxy.server.proxyservice.client.listener.ResponseListener;
import com.virjar.dungproxy.server.proxyservice.common.Constants;
import com.virjar.dungproxy.server.proxyservice.common.util.NetworkUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpConstants;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.List;

import static com.virjar.dungproxy.server.proxyservice.client.decoder.HttpBodyDecoder.State.NO_CONTENT;


public class HttpHeaderDecoder extends ReplayingDecoder<HttpHeaderDecoder.State> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpHeaderDecoder.class);

    public final static AttributeKey<Boolean> KEEP_ALIVE = AttributeKey.valueOf("keep-alive");

    private static final String CR_LF = String.valueOf(new char[]{HttpConstants.CR, HttpConstants.LF});

    protected static final ThreadLocal<StringBuilder> BUILDERS = new ThreadLocal<StringBuilder>() {

        @Override
        public StringBuilder initialValue() {
            return new StringBuilder(512);
        }

        @Override
        public StringBuilder get() {
            StringBuilder sb = super.get();
            sb.setLength(0);
            return sb;
        }
    };

    private ResponseListener listener;

    private final int maxInitialLineLength;
    private final int maxHeaderSize;
    private HttpResponse message;
    private int headerSize;
    private ByteBuf initialLineBuf;
    private boolean decodeFinished = false;
    private boolean shouldKeepConnectionAlive = false;
    private ByteBuf headerBuf;
    private boolean retry;

    private Proxy proxy;

    enum State {
        SKIP_CONTROL_CHARS,
        READ_INITIAL,
        READ_HEADER,
        READ_VARIABLE_LENGTH_CONTENT,
        READ_FIXED_LENGTH_CONTENT,
        READ_CHUNK
    }

    /**
     * Creates a new instance with the default
     * {@code maxInitialLineLength (4096}}, {@code maxHeaderSize (8192)}
     */
    public HttpHeaderDecoder(ResponseListener listener, Proxy proxy, boolean retry) {
        this(4096, 8192, listener, retry, proxy);
    }

    /**
     * Creates a new instance with the specified parameters.
     */
    public HttpHeaderDecoder(
            int maxInitialLineLength,
            int maxHeaderSize,
            ResponseListener listener,
            boolean retry, Proxy proxy) {

        super(State.SKIP_CONTROL_CHARS);
        this.retry = retry;
        this.proxy = proxy;

        if (maxInitialLineLength <= 0) {
            throw new IllegalArgumentException(
                    "maxInitialLineLength must be a positive integer: " +
                            maxInitialLineLength
            );
        }
        if (maxHeaderSize <= 0) {
            throw new IllegalArgumentException(
                    "maxHeaderSize must be a positive integer: " +
                            maxHeaderSize
            );
        }
        this.maxInitialLineLength = maxInitialLineLength;
        this.maxHeaderSize = maxHeaderSize;
        this.listener = listener;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        switch (state()) {
            case SKIP_CONTROL_CHARS:
                try {
                    skipControlCharacters(in);
                    checkpoint(State.READ_INITIAL);
                } finally {
                    checkpoint();
                }
            case READ_INITIAL:
                try {
                    String[] initialLine = splitInitialLine(readLine(in, maxInitialLineLength));
                    if (initialLine.length < 3) {
                        // Invalid initial line - ignore
                        finish(ctx, new BadMessageHandler(listener, new IllegalStateException("Invalid initial line")), in);
                        return;
                    }

                    message = createHeader(initialLine);
                    checkpoint(State.READ_HEADER);
                    if (in.readerIndex() == in.writerIndex()) {
                        initialLineBuf = in.copy(0, in.writerIndex());
                        initialLineBuf.readerIndex(0);
                    }
                } catch (Exception e) {
                    // bad message, initial line too long e.g.
                    finish(ctx, new BadMessageHandler(listener, e), in);
                    return;
                }
            case READ_HEADER:
                try {
                    State nextState = readHeaders(in);
                    LOGGER.debug("Header received [{}]", message);
                    boolean needRetry = listener.onHeaderReceived(message);
                    if (needRetry && retry) {
                        // return to retry
                        ctx.pipeline().remove(this);
                        in.readerIndex(in.writerIndex());
                        NetworkUtil.releaseMsgCompletely(initialLineBuf);
                        NetworkUtil.releaseMsgCompletely(headerBuf);
                        return;
                    }
                    checkpoint(nextState);

                    headerBuf = createHeaderBuf(in, Constants.PROXY_ROUTER_KEY, String.valueOf(proxy.getId()), in.readerIndex());
                    if (nextState == State.SKIP_CONTROL_CHARS) {
                        // No content is expected
                        finish(ctx, new HttpBodyNoContentDecoder(listener, NO_CONTENT, headerBuf, headerBuf.readerIndex()), in);
                        return;
                    }

                    long contentLength = HttpHeaders.getContentLength(message, -1);

                    // Hand over the unhandled data to next handler
                    switch (nextState) {
                        case READ_CHUNK: {
                            LOGGER.debug("Decode successfully, body type is CHUNKED");
                            finish(ctx, new HttpBodyChunkDecoder(listener, headerBuf, headerBuf.readerIndex()), in);
                            break;
                        }
                        case READ_FIXED_LENGTH_CONTENT: {
                            LOGGER.debug("Decode successfully, body type is FIX_LENGTH, length is {}", contentLength);
                            finish(ctx, new HttpBodyFixedLengthDecoder(listener, headerBuf, contentLength, headerBuf.readerIndex()), in);
                            break;
                        }
                        case READ_VARIABLE_LENGTH_CONTENT: {
                            LOGGER.debug("Decode successfully, body type is VARI_LENGTH");
                            finish(ctx, new HttpBodyVariLengthDecoder(listener, headerBuf, headerBuf.readerIndex()), in);
                            break;
                        }
                        default: {
                            NetworkUtil.releaseMsgCompletely(headerBuf);
                            throw new IllegalStateException("Unexpected state: " + nextState);
                        }
                    }
                    // We return here, this forces decode to be called again where we will decode the content
                } catch (Exception e) {
                    if (headerBuf != null) {
                        NetworkUtil.releaseMsgCompletely(headerBuf);
                    }
                    finish(ctx, new BadMessageHandler(listener, e), in);
                }
                break;
        }
    }

    /**
     * Return a new ByteBuf which contains the specific header
     *
     * @param src            original byte buf
     * @param key            header key
     * @param value          header val
     * @param headerEndIndex the index of the end of last header
     * @return ByteBuf contains the specific header
     */
    private ByteBuf createHeaderBuf(ByteBuf src, String key, String value, int headerEndIndex) {
        byte[] insertion = (key + ": " + value + CR_LF).getBytes();
        ByteBuf newBuf;
        int initBufLen = 0;
        if (initialLineBuf != null) {
            initBufLen = initialLineBuf.writerIndex();
            newBuf = ByteBufAllocator.DEFAULT.directBuffer(src.writerIndex() + initialLineBuf.writerIndex() + insertion.length);
            newBuf.writeBytes(initialLineBuf);
            initialLineBuf.release();
            initialLineBuf = null;
        } else {
            newBuf = ByteBufAllocator.DEFAULT.directBuffer(src.writerIndex() + insertion.length);
        }
        newBuf.writeBytes(src, 0, headerEndIndex - 2);
        newBuf.writeBytes(insertion);
        newBuf.writeBytes(src, headerEndIndex - 2, src.writerIndex() - headerEndIndex + 2);
        newBuf.readerIndex(headerEndIndex + insertion.length + initBufLen);
        return newBuf;
    }

    /**
     * It's called once the http headers is decoded.
     * The decode task will be ended and hand over to {@link HttpBodyDecoder} handler
     */
    private void finish(
            ChannelHandlerContext ctx,
            ChannelHandler nextHandler,
            ByteBuf cumulation
    ) {
        // Set reader index to the writer index in order to release it in ByteToMessageDecoder
        ctx.channel().attr(KEEP_ALIVE).set(shouldKeepConnectionAlive);
        cumulation.readerIndex(cumulation.writerIndex());
        decodeFinished = true;
        ctx.pipeline().remove(this);
        ctx.pipeline().addLast(nextHandler);
    }

    private static String[] splitInitialLine(StringBuilder sb) {
        int protocolStart;
        int protocolEnd;
        int codeStart;
        int codeEnd;
        int textStart;
        int textEnd;

        protocolStart = findNonWhitespace(sb, 0);
        protocolEnd = findWhitespace(sb, protocolStart);

        codeStart = findNonWhitespace(sb, protocolEnd);
        codeEnd = findWhitespace(sb, codeStart);

        textStart = findNonWhitespace(sb, codeEnd);
        textEnd = findLastNonWhitespace(sb);

        return new String[]{
                sb.substring(protocolStart, protocolEnd),
                sb.substring(codeStart, codeEnd),
                textStart < textEnd ? sb.substring(textStart, textEnd) : ""};
    }

    private StringBuilder readHeader(ByteBuf buf) {
        StringBuilder sb = BUILDERS.get();
        int headerSize = this.headerSize;

        loop:
        for (; ; ) {
            char nextByte = (char) buf.readByte();
            headerSize++;

            switch (nextByte) {
                case HttpConstants.CR:
                    nextByte = (char) buf.readByte();
                    headerSize++;
                    if (nextByte == HttpConstants.LF) {
                        break loop;
                    }
                    break;
                case HttpConstants.LF:
                    break loop;
            }

            // Abort decoding if the message part is too large
            if (headerSize >= maxHeaderSize) {
                throw new TooLongFrameException("HTTP message is larger than " + maxHeaderSize + " bytes.");
            }
            sb.append(nextByte);
        }

        this.headerSize = headerSize;
        return sb;
    }

    /**
     * Create a response message with http initial line
     *
     * @param initialLine http initial line
     * @return message Object
     * @throws Exception
     */
    private HttpResponse createHeader(String[] initialLine) throws Exception {
        return new DefaultHttpResponse(
                HttpVersion.valueOf(initialLine[0]),
                new HttpResponseStatus(Integer.valueOf(initialLine[1]), initialLine[2]));
    }

    private State readHeaders(ByteBuf buf) {
        headerSize = 0;
        final HttpMessage message = this.message;
        final HttpHeaders headers = message.headers();

        StringBuilder line = readHeader(buf);
        String hName = null;
        String hVal = null;
        if (line.length() > 0) {
            headers.clear();
            do {
                char firstChar = line.charAt(0);
                if (hName != null && (firstChar == ' ' || firstChar == '\t')) {
                    hVal = hVal + ' ' + line.toString().trim();
                } else {
                    if (hName != null) {
                        headers.add(hName, hVal);
                    }
                    String[] currentHeader = splitHeader(line);
                    hName = currentHeader[0];
                    hVal = currentHeader[1];
                }
                line = readHeader(buf);
            } while (line.length() > 0);

            // last message
            if (hName != null) {
                headers.add(hName, hVal);
            }
        }

        State nextState;

        if (isContentAlwaysEmpty(message)) {
            HttpHeaders.removeTransferEncodingChunked(message);
            nextState = State.SKIP_CONTROL_CHARS;
        } else if (HttpHeaders.isTransferEncodingChunked(message)) {
            nextState = State.READ_CHUNK;
        } else if (HttpHeaders.getContentLength(message, -1) >= 0) {
            nextState = State.READ_FIXED_LENGTH_CONTENT;
        } else {
            nextState = State.READ_VARIABLE_LENGTH_CONTENT;
        }

        String connectionHeader = headers.get("Connection");
        shouldKeepConnectionAlive = nextState != State.READ_VARIABLE_LENGTH_CONTENT && !message.getProtocolVersion().equals(HttpVersion.HTTP_1_0) && ((connectionHeader == null) || (connectionHeader.equalsIgnoreCase("keep-alive")));
        LOGGER.debug("shouldKeepConnectionAlive:{}", shouldKeepConnectionAlive);

        return nextState;
    }

    private static boolean isContentAlwaysEmpty(HttpMessage msg) {
        if (msg instanceof HttpResponse) {
            HttpResponse res = (HttpResponse) msg;
            int code = res.getStatus().code();

            // handle 1xx
            if (code >= 100 && code < 200) {
                return !(code == 101 && !res.headers().contains(HttpHeaders.Names.SEC_WEBSOCKET_ACCEPT));
            }
            switch (code) {
                case 204:
                case 205:
                case 304:
                    return true;
            }
        }

        return false;
    }

    private static String[] splitHeader(StringBuilder sb) {
        final int length = sb.length();
        int nameStart;
        int nameEnd;
        int colonEnd;
        int valueStart;
        int valueEnd;

        nameStart = findNonWhitespace(sb, 0);
        for (nameEnd = nameStart; nameEnd < length; nameEnd++) {
            char ch = sb.charAt(nameEnd);
            if (ch == ':' || Character.isWhitespace(ch)) {
                break;
            }
        }

        for (colonEnd = nameEnd; colonEnd < length; colonEnd++) {
            if (sb.charAt(colonEnd) == ':') {
                colonEnd++;
                break;
            }
        }

        valueStart = findNonWhitespace(sb, colonEnd);
        if (valueStart == length) {
            return new String[]{sb.substring(nameStart, nameEnd), ""};
        }

        valueEnd = findLastNonWhitespace(sb);
        return new String[]{
                sb.substring(nameStart, nameEnd),
                sb.substring(valueStart, valueEnd)
        };
    }

    /**
     * read a line from {@code buf} util a <i>CR LF</i> or <i>LF</i> is encountered
     *
     * @param buf           source buf
     * @param maxLineLength max line length
     * @return a {@code StringBuilder} represents the line text
     */
    private static StringBuilder readLine(ByteBuf buf, int maxLineLength) {
        StringBuilder sb = BUILDERS.get();
        int lineLength = 0;
        while (true) {
            byte nextByte = buf.readByte();
            if (nextByte == HttpConstants.CR) {
                nextByte = buf.readByte();
                if (nextByte == HttpConstants.LF) {
                    return sb;
                }
            } else if (nextByte == HttpConstants.LF) {
                return sb;
            } else {
                if (lineLength >= maxLineLength) {
                    throw new TooLongFrameException(
                            "An HTTP line is larger than " + maxLineLength +
                                    " bytes."
                    );
                }
                lineLength++;
                sb.append((char) nextByte);
            }
        }
    }

    private static void skipControlCharacters(ByteBuf buf) {
        for (; ; ) {
            char c = (char) buf.readUnsignedByte();
            if (!Character.isISOControl(c) && !Character.isWhitespace(c)) {
                buf.readerIndex(buf.readerIndex() - 1);
                break;
            }
        }
    }

    /**
     * find the first non whitespace char of {@code cs} from {@code start} on
     *
     * @param cs    target char sequence
     * @param start index to start
     * @return index of the first non whitespace char
     */
    private static int findNonWhitespace(CharSequence cs, int start) {
        int index;
        for (index = start; index < cs.length(); index++) {
            if (!Character.isWhitespace(cs.charAt(index))) {
                break;
            }
        }
        return index;
    }

    /**
     * find the first whitespace char of {@code cs} from {@code start} on
     *
     * @param cs    target char sequence
     * @param start index to start
     * @return index of the first whitespace char
     */
    private static int findWhitespace(CharSequence cs, int start) {
        int index;
        for (index = start; index < cs.length(); index++) {
            if (Character.isWhitespace(cs.charAt(index))) {
                break;
            }
        }
        return index;
    }

    /**
     * find the last non whitespace char of {@code cs}
     *
     * @param cs target char sequence
     * @return index of the last non whitespace char
     */
    private static int findLastNonWhitespace(CharSequence cs) {
        int index;
        for (index = cs.length(); index > 0; index--) {
            if (!Character.isWhitespace(cs.charAt(index - 1))) {
                break;
            }
        }
        return index;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.debug("Exception occurred while decoding response headers", cause);
        NetworkUtil.releaseMsgCompletely(initialLineBuf);
        if (!decodeFinished) {
            NetworkUtil.releaseMsgCompletely(headerBuf);
        }
        listener.onThrowable(getClass().getName() + " exceptionCaught", cause);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        NetworkUtil.releaseMsgCompletely(initialLineBuf);
        if (!decodeFinished) {
            String msg = "Server channel inactive while decoding response header";
            LOGGER.debug(msg);
            NetworkUtil.releaseMsgCompletely(headerBuf);
            listener.onThrowable(msg, ServerChannelInactiveException.INSTANCE);
        }
        super.channelInactive(ctx);
    }
}

