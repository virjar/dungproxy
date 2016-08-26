package com.virjar.core.exception;

/**
 * The business exception used to throw out exception which related to business logic
 * <p>
 * The service should only throw out business exception and catch other exception(e.g. SQL exception) to transform it as
 * {@link BusinessException} or the exception which inherit it
 * <p>
 * <b>errorCode</b> is used to identity the exception, and also will be used as localization code
 * <p>
 * <b>message</b> is used to store exception description for development(log)
 * 
 * @author weijia.deng
 */
public class BusinessException extends RuntimeException {
    private static final long serialVersionUID = 8136282319218571410L;

    protected final String errorCode;

    public BusinessException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}