package com.virjar.dungproxy.server.core.utils;

public class ReturnInfo {
    public static final String SUCCEED = "0";

    private String statusCode;

    private String thirdpartyStatusCode;

    private String message;

    public ReturnInfo(String statusCode) {
        super();
        this.statusCode = statusCode;
    }

    public ReturnInfo(String statusCode, String message) {
        super();
        this.statusCode = statusCode;
        this.message = message;
    }

    public ReturnInfo() {
        super();
    }

    public String getThirdpartyStatusCode() {
        return thirdpartyStatusCode;
    }

    public void setThirdpartyStatusCode(String thirdpartyStatusCode) {
        this.thirdpartyStatusCode = thirdpartyStatusCode;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}