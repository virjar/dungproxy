package com.virjar.proxyservice.handler.checker;

import com.virjar.proxyservice.common.util.NetworkUtil;
import com.virjar.proxyservice.handler.ClientProcessHandler;
import com.virjar.proxyservice.handler.DrungProxyHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.virjar.proxyservice.common.Constants.CUSTOM_USER_AGENT_KEY;
import static com.virjar.proxyservice.common.Constants.PROXY_HEADER_SET;
import static com.virjar.proxyservice.common.Constants.USE_HTTPS_KEY;
import static io.netty.util.AttributeKey.valueOf;

/**
 * Description: RequestChecker
 *
 * @author lingtong.fu
 * @version 2016-10-31 02:56
 */
@ChannelHandler.Sharable
public class RequestValidator extends ClientProcessHandler {

    private static final Logger log = LoggerFactory.getLogger(RequestValidator.class);

    private static final int DEFAULT_REQUEST_TIMEOUT = 60000;

    private static final int MAX_REQUEST_TIMEOUT = 10 * DEFAULT_REQUEST_TIMEOUT;



    public static final RequestValidator instance = new RequestValidator();


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        try {
            HttpRequest request = (HttpRequest) msg;
            HttpVersion version = request.protocolVersion();
            HttpMethod method = request.method();

            log.info("收到请求 [CH] [{}] Request [{}] [{}] [{}]", ctx.channel(), version, method, NetworkUtil.requestToCurl(request));

            setIfHttps(request);
            checkHttpMethod(request);
            checkExpected100Continue(request);
            setHttpVersion(request);
            checkUserAgent(request, ctx);
            clearQProxyHeaders(request);
            //TODO 代理请求处理
            NetworkUtil.resetHandler(ctx.pipeline(), new DrungProxyHandler());

            super.channelRead(ctx, msg);
        } catch (Exception e) {
            NetworkUtil.releaseMsgCompletely(msg);
            throw e;
        }
    }

    private void setIfHttps(HttpRequest request) {
        String uri = request.uri();
        String useHttps = request.headers().get(USE_HTTPS_KEY);
        if (useHttps != null && useHttps.equals("1")) {
            if (uri.substring(0, 5).toLowerCase().equals("http:")) {
                uri = uri.substring(0, 4) + "s" + uri.substring(4);
            }
        }
        request.setUri(uri);
    }

    private void checkHttpMethod(HttpRequest request) {
        if (request.method().equals(HttpMethod.GET) && request.headers().get(HttpHeaderNames.CONTENT_LENGTH) != null) {
            request.headers().remove(HttpHeaderNames.CONTENT_LENGTH);
        }
    }

    //支持目标网站的HTTP的版本号为1.0或之前的版本
    private void checkExpected100Continue(HttpRequest request) {
        if (HttpUtil.is100ContinueExpected(request)) {
            request.headers().remove(HttpHeaderNames.EXPECT);
        }
    }

    private void setHttpVersion(HttpRequest request) {
        request.setProtocolVersion(HttpVersion.HTTP_1_1);
    }

    private void checkUserAgent(HttpRequest request, ChannelHandlerContext ctx) {
        String userAgent = request.headers().get(CUSTOM_USER_AGENT_KEY);
        if (userAgent != null) {
            NetworkUtil.setAttr(ctx.channel(), valueOf("cusUserAgent"), true);
        }
    }

    private void clearQProxyHeaders(HttpRequest request) {
        for (String header : PROXY_HEADER_SET) {
            request.headers().remove(header);
        }
    }
}
