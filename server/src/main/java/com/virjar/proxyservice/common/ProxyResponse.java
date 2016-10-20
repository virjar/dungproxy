package com.virjar.proxyservice.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

/**
 * Description: ProxyResponse
 *
 * @author lingtong.fu
 * @version 2016-10-18 18:00
 */
public class ProxyResponse extends DefaultFullHttpResponse {

    private static final ByteBuf RESPONSE_BODY = Unpooled.unreleasableBuffer(Unpooled.EMPTY_BUFFER);

    private static final HttpResponseStatus FEEDBACK_SUCCESS_STATUS = new HttpResponseStatus(200, "PROXY FEEDBACK SUCCESS");
    private static final HttpResponseStatus RAW_HTTPS_DENIED = new HttpResponseStatus(403, "PROXY RAW HTTPS DENIED");

    public static final ProxyResponse FEEDBACK_SUCCESS_RESPONSE = new ProxyResponse(FEEDBACK_SUCCESS_STATUS);
    public static final ProxyResponse RAW_HTTPS_DENIED_RESPONSE = new ProxyResponse(RAW_HTTPS_DENIED);

    public ProxyResponse(HttpResponseStatus responseStatus) {
        super(HttpVersion.HTTP_1_1, responseStatus, RESPONSE_BODY);
        headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/plain; charset=utf-8");
        headers().set(HttpHeaders.Names.CONTENT_LENGTH, 0);
        headers().set(HttpHeaders.Names.CONNECTION, "close");
    }

    public static ProxyResponse proxyError(long proxyId, String message, String traceId) {
        ProxyResponse resp = new ProxyResponse(new HttpResponseStatus(417, message));
        resp.headers().set("Proxy-Router", proxyId);
        resp.headers().set("Proxy-Error-Trace", traceId);
        return resp;
    }

}
