package com.virjar.dungproxy.server.proxyservice.client.exception;

import java.io.IOException;

public class ServerChannelNotWritableException extends IOException {

    public static final ServerChannelNotWritableException INSTANCE = new ServerChannelNotWritableException();

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    @Override
    public synchronized Throwable initCause(Throwable cause) {
        return this;
    }
}
