package com.virjar.dungproxy.server.proxyservice.client.exception;

public class EmptyException extends Exception {

    public EmptyException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable initCause(Throwable cause) {
        return this;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
