package com.virjar.dungproxy.server.proxyservice.client.exception;

import java.io.IOException;

public class ServerChannelInactiveException extends IOException {

    public static final ServerChannelInactiveException INSTANCE = new ServerChannelInactiveException();

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    @Override
    public synchronized Throwable initCause(Throwable cause) {
        return this;
    }
}
