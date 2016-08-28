package com.virjar.core.exception;

import java.util.Arrays;

/**
 * The validation exception which used as {@link LocalizableBusinessException}, but make the other code to identity the
 * exception(e.g. web exception handler will use {@link ValidationException} as identity to response 400 bad request in
 * REST request)
 * 
 * @author weijia.deng
 */
public class ValidationException extends LocalizableBusinessException {
    private static final long serialVersionUID = -2441669274890726789L;

    public ValidationException(String message, String errorCode) {
        super(message, errorCode);
    }

    public ValidationException(String message, String errorCode, Object[] arguments) {
        super(message, errorCode);
        this.arguments = Arrays.copyOf(arguments, arguments.length);
    }

    public ValidationException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }

    public ValidationException(String message, String errorCode, Object[] arguments, Throwable cause) {
        super(message, errorCode, cause);
        this.arguments = Arrays.copyOf(arguments, arguments.length);
    }
}