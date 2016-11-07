package com.virjar.dungproxy.server.proxyservice.client.exception;

public abstract class HttpDecodeFailException extends Exception {

    protected HttpDecodeFailException(Throwable cause) {
        super(cause);
    }
}
