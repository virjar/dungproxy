package com.virjar.core.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * The rest API error model
 * 
 * @author weijia.deng
 */
@JsonInclude(Include.NON_EMPTY)
public class RestApiError {
    private String statusCode;

    private String message;

    private String rawMessage;

    private ValidationError validationError;

    private String moreInfo;

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
        this.message = message;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
        this.rawMessage = rawMessage;
    }

    public ValidationError getValidationError() {
        return validationError;
    }

    public void setValidationError(ValidationError validationError) {
        this.validationError = validationError;
        this.validationError = validationError;
    }

    public String getMoreInfo() {
        return moreInfo;
    }

    public void setMoreInfo(String moreInfo) {
        this.moreInfo = moreInfo;
        this.moreInfo = moreInfo;
    }
}