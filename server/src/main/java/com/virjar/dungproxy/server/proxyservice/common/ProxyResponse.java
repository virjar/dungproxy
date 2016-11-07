package com.virjar.dungproxy.server.proxyservice.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;

import static com.virjar.dungproxy.server.proxyservice.common.Constants.PROXY_ROUTER_KEY;

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
    private static final HttpResponseStatus NO_AVAILABLE_PROXY_STATUS = new HttpResponseStatus(417, "PROXY NO AVAILABLE PROXY");
    private static final HttpResponseStatus TOO_MANY_CONNECTION_STATUS = new HttpResponseStatus(500, "QPROXY CONNECTION POOL IS FULL");
    private static final HttpResponseStatus PROXY_TIMEOUT_STATUS = new HttpResponseStatus(504, "QPROXY SERVER TIME OUT");

    public static final ProxyResponse FEEDBACK_SUCCESS_RESPONSE = new ProxyResponse(FEEDBACK_SUCCESS_STATUS);
    public static final ProxyResponse RAW_HTTPS_DENIED_RESPONSE = new ProxyResponse(RAW_HTTPS_DENIED);
    public static final ProxyResponse TOO_MANY_CONNECTION_RESPONSE = new ProxyResponse(TOO_MANY_CONNECTION_STATUS);

    public ProxyResponse(HttpResponseStatus responseStatus) {
        super(HttpVersion.HTTP_1_1, responseStatus, RESPONSE_BODY);
        headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=utf-8");
        headers().set(HttpHeaderNames.CONTENT_LENGTH, 0);
        headers().set(HttpHeaderNames.CONNECTION, "close");
    }

    public static ProxyResponse proxyError(long proxyId, String message, String traceId) {
        ProxyResponse resp = new ProxyResponse(new HttpResponseStatus(417, message));
        resp.headers().set("Proxy-Router", proxyId);
        resp.headers().set("Proxy-Error-Trace", traceId);
        return resp;
    }

    public static ProxyResponse noAvailableProxy() {
        return new ProxyResponse(NO_AVAILABLE_PROXY_STATUS);
    }

    public static ProxyResponse proxyTimeout(long proxyId) {
        ProxyResponse response = new ProxyResponse(PROXY_TIMEOUT_STATUS);
        response.headers().add(PROXY_ROUTER_KEY, proxyId);
        return response;
    }

}
