package com.virjar.core.exception;

/**
 * Created by virjar on 16/4/10.
 */
public class ServerInternalException extends RuntimeException {
    private static final long serialVersionUID = 8136282319218571410L;

    protected final String errorCode;

    public ServerInternalException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public ServerInternalException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}