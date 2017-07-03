package com.virjar.dungproxy.client.httpclient.conn.logging;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by virjar on 17/7/3. <br/>
 * 
 * @since 0.0.8
 * @author virjar
 */
class LoggingInputStream extends InputStream {

    private final InputStream in;
    private final Wire wire;

    public LoggingInputStream(final InputStream in, final Wire wire) {
        super();
        this.in = in;
        this.wire = wire;
    }

    @Override
    public int read() throws IOException {
        try {
            final int b = in.read();
            if (b == -1) {
                wire.input("end of stream");
            } else {
                wire.input(b);
            }
            return b;
        } catch (IOException ex) {
            wire.input("[read] I/O error: " + ex.getMessage());
            throw ex;
        }
    }

    @Override
    public int read(final byte[] b) throws IOException {
        try {
            final int bytesRead = in.read(b);
            if (bytesRead == -1) {
                wire.input("end of stream");
            } else if (bytesRead > 0) {
                wire.input(b, 0, bytesRead);
            }
            return bytesRead;
        } catch (IOException ex) {
            wire.input("[read] I/O error: " + ex.getMessage());
            throw ex;
        }
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
        try {
            final int bytesRead = in.read(b, off, len);
            if (bytesRead == -1) {
                wire.input("end of stream");
            } else if (bytesRead > 0) {
                wire.input(b, off, bytesRead);
            }
            return bytesRead;
        } catch (IOException ex) {
            wire.input("[read] I/O error: " + ex.getMessage());
            throw ex;
        }
    }

    @Override
    public long skip(final long n) throws IOException {
        try {
            return super.skip(n);
        } catch (IOException ex) {
            wire.input("[skip] I/O error: " + ex.getMessage());
            throw ex;
        }
    }

    @Override
    public int available() throws IOException {
        try {
            return in.available();
        } catch (IOException ex) {
            wire.input("[available] I/O error : " + ex.getMessage());
            throw ex;
        }
    }

    @Override
    public void mark(final int readlimit) {
        super.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
        super.reset();
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    @Override
    public void close() throws IOException {
        try {
            in.close();
        } catch (IOException ex) {
            wire.input("[close] I/O error: " + ex.getMessage());
            throw ex;
        }
    }

}