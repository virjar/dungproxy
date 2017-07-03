package com.virjar.dungproxy.client.httpclient.conn.logging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import org.apache.commons.logging.Log;
import org.apache.http.Header;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.config.MessageConstraints;
import org.apache.http.entity.ContentLengthStrategy;
import org.apache.http.impl.conn.DefaultManagedHttpClientConnection;
import org.apache.http.io.HttpMessageParserFactory;
import org.apache.http.io.HttpMessageWriterFactory;

/**
 * Created by virjar on 17/7/3.
 */
public class PlainTextLoggingManagedHttpClientConnection extends DefaultManagedHttpClientConnection {

    private final Log log;
    private final Wire wire;

    public PlainTextLoggingManagedHttpClientConnection(final String id, final Log log,
            final Log wirelog, final int buffersize, final int fragmentSizeHint, final CharsetDecoder chardecoder,
            final CharsetEncoder charencoder, final MessageConstraints constraints,
            final ContentLengthStrategy incomingContentStrategy, final ContentLengthStrategy outgoingContentStrategy,
            final HttpMessageWriterFactory<HttpRequest> requestWriterFactory,
            final HttpMessageParserFactory<HttpResponse> responseParserFactory) {
        super(id, buffersize, fragmentSizeHint, chardecoder, charencoder, constraints, incomingContentStrategy,
                outgoingContentStrategy, requestWriterFactory, responseParserFactory);
        this.log = log;
        this.wire = new Wire(wirelog, id);
    }

    @Override
    public void close() throws IOException {
        if (this.log.isDebugEnabled()) {
            this.log.debug(getId() + ": Close connection");
        }
        super.close();
    }

    @Override
    public void shutdown() throws IOException {
        if (this.log.isDebugEnabled()) {
            this.log.debug(getId() + ": Shutdown connection");
        }
        super.shutdown();
    }

    @Override
    protected InputStream getSocketInputStream(final Socket socket) throws IOException {
        InputStream in = super.getSocketInputStream(socket);
        in = new LoggingInputStream(in, this.wire);
        return in;
    }

    @Override
    protected OutputStream getSocketOutputStream(final Socket socket) throws IOException {
        OutputStream out = super.getSocketOutputStream(socket);
        out = new LoggingOutputStream(out, this.wire);
        return out;
    }

    @Override
    protected void onResponseReceived(final HttpResponse response) {
        if (response != null) {
            System.out.println(response.getStatusLine().toString());
            final Header[] headers = response.getAllHeaders();
            for (final Header header : headers) {
                System.out.println(header.toString());
            }
        }
    }

    @Override
    protected void onRequestSubmitted(final HttpRequest request) {
        if (request != null) {
            System.out.println(request.getRequestLine().toString());
            final Header[] headers = request.getAllHeaders();
            for (final Header header : headers) {
                System.out.println(header.toString());
            }
        }
    }
}
