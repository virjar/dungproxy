package com.virjar.dungproxy.client.httpclient.conn.logging;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.http.util.Args;

/**
 * Created by virjar on 17/7/4.<br/>
 * 默认把日子输出到控制台
 * 
 * @since 0.0.8
 * @author virjar
 */
public class Wire {
    private final Log log;
    private final String id;
    private boolean logHex = false;

    /**
     * @since 4.3
     */
    public Wire(final Log log, final String id) {
        this.log = log;
        this.id = id;
    }

    public void wire(final String header, final InputStream instream) throws IOException {
        if (!logHex) {
            System.out.println(IOUtils.toString(instream));
            return;
        }

        final StringBuilder buffer = new StringBuilder();
        int ch;
        while ((ch = instream.read()) != -1) {
            buffer.append(Integer.toHexString(ch));
        }
        System.out.println(buffer.toString());
    }

    public boolean enabled() {
        return log.isDebugEnabled();
    }

    public void output(final InputStream outstream) throws IOException {
        Args.notNull(outstream, "Output");
        wire("", outstream);
    }

    public void input(final InputStream instream) throws IOException {
        Args.notNull(instream, "Input");
        wire("    ", instream);
    }

    public void output(final byte[] b, final int off, final int len) throws IOException {
        Args.notNull(b, "Output");
        wire("", new ByteArrayInputStream(b, off, len));
    }

    public void input(final byte[] b, final int off, final int len) throws IOException {
        Args.notNull(b, "Input");
        wire("   ", new ByteArrayInputStream(b, off, len));
    }

    public void output(final byte[] b) throws IOException {
        Args.notNull(b, "Output");
        wire("", new ByteArrayInputStream(b));
    }

    public void input(final byte[] b) throws IOException {
        Args.notNull(b, "Input");
        wire("   ", new ByteArrayInputStream(b));
    }

    public void output(final int b) throws IOException {
        output(new byte[] { (byte) b });
    }

    public void input(final int b) throws IOException {
        input(new byte[] { (byte) b });
    }

    public void output(final String s) throws IOException {
        Args.notNull(s, "Output");
        output(s.getBytes());
    }

    public void input(final String s) throws IOException {
        Args.notNull(s, "Input");
        input(s.getBytes());
    }
}
