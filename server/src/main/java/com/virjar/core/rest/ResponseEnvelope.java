package com.virjar.core.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.virjar.core.exception.RestApiError;

/**
 * The response envelope to envelope the response data and make it as the standard data
 * 
 * @author weijia.deng
 * @param < T >
 */
@JsonInclude(Include.NON_EMPTY)
public class ResponseEnvelope<T> {
    private T data;

    private PageInfo pageInfo;

    private RestApiError error;

    private boolean status;

    public ResponseEnvelope() {
        this(null, null, false);
    }

    public ResponseEnvelope(T data) {
        this(data, null, false);
    }

    public ResponseEnvelope(T data, PageInfo pageInfo, boolean status) {
        this.data = data;
        this.pageInfo = pageInfo;
        this.status = status;
    }

    public ResponseEnvelope(RestApiError error) {
        this.error = error;
    }

    public ResponseEnvelope(T data, boolean status) {
        this(data, null, status);
    }

    public ResponseEnvelope(RestApiError error, boolean status) {
        this.error = error;
        this.status = status;
    }

    public T getData() {
        return data;
    }

    public PageInfo getPageInfo() {
        return pageInfo;
    }

    public RestApiError getError() {
        return error;
    }

    public boolean isStatus() {
        return status;
    }
}