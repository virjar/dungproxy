package com.virjar.utils.net;

public class HttpResult {

    public int statusCode;
    public String location;
    public String responseBody;

    public HttpResult(int statusCode, String location, String responseBody) {
        super();
        this.statusCode = statusCode;
        this.location = location;
        this.responseBody = responseBody;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public HttpResult(int statusCode) {
        super();
        this.statusCode = statusCode;
    }

    public HttpResult(int statusCode, String responseBody) {
        super();
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

}
