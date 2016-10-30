package com.virjar.proxyservice.handler.checker;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.virjar.proxyservice.common.ProxyResponse.RAW_HTTPS_DENIED_RESPONSE;
import static com.virjar.proxyservice.common.util.NetworkUtil.writeAndFlushAndClose;

/**
 * Description: ConnectMethodValidator
 *
 * @author lingtong.fu
 * @version 2016-10-19 20:39
 */
@ChannelHandler.Sharable
public class ConnectMethodValidator extends ValidateHandler<HttpMethod> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectMethodValidator.class);

    public static final ConnectMethodValidator instance = new ConnectMethodValidator();

    private ConnectMethodValidator() {
    }

    @Override
    public int validate(ChannelHandlerContext ctx, HttpMethod method) {
        return method.equals(HttpMethod.CONNECT) ? -1 : 1;
    }

    @Override
    public void onValidateFail(ChannelHandlerContext ctx, int ret, HttpMethod param, FullHttpRequest request) {
        LOGGER.info("[{}] [URL] [{}] 发送 CONNECT 请求", ctx.channel(), request.getUri());
        writeAndFlushAndClose(ctx.channel(), RAW_HTTPS_DENIED_RESPONSE);
    }

    @Override
    public void onValidateSuccess(ChannelHandlerContext ctx, int ret, HttpMethod param) {

    }

    @Override
    public HttpMethod getValidateParam(ChannelHandlerContext ctx, HttpRequest request) {
        return request.getMethod();
    }
}
