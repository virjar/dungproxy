package com.virjar.dungproxy.server.proxyservice.handler.checker;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;

/**
 * Description: 验证参数是否合法
 *
 * @author lingtong.fu
 * @version 2016-10-19 20:37
 */
public interface Validator<T> {
    /**
     * @return 合法返回正数, 否则返回负数
     */
    int validate(ChannelHandlerContext ctx, T param);

    void onValidateFail(ChannelHandlerContext ctx, int ret, T param, FullHttpRequest request);

    void onValidateSuccess(ChannelHandlerContext ctx, int ret, T param);

    T getValidateParam(ChannelHandlerContext ctx, HttpRequest request);
}
