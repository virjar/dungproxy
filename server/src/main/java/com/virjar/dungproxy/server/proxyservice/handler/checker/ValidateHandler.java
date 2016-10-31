package com.virjar.dungproxy.server.proxyservice.handler.checker;

import com.virjar.dungproxy.server.proxyservice.common.util.NetworkUtil;
import com.virjar.dungproxy.server.proxyservice.handler.ClientProcessHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpRequest;

/**
 * Description: ValidateHandler
 *
 * @author lingtong.fu
 * @version 2016-10-19 20:37
 */
@ChannelHandler.Sharable
public abstract class ValidateHandler<T> extends ClientProcessHandler implements Validator<T> {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            FullHttpRequest request = (FullHttpRequest) msg;
            T param = getValidateParam(ctx, request);
            int ret = validate(ctx, param);

            if (ret >= 0) {
                onValidateSuccess(ctx, ret, param);
                super.channelRead(ctx, msg);
            } else {
                NetworkUtil.releaseMsgCompletely(msg);
                onValidateFail(ctx, ret, param, request);
            }
        } catch (Exception e) {
            NetworkUtil.releaseMsgCompletely(msg);
            throw e;
        }
    }
}
