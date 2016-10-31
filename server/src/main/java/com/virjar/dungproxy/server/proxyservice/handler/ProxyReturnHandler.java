package com.virjar.dungproxy.server.proxyservice.handler;

import com.virjar.dungproxy.server.proxyservice.common.util.NetworkUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

import java.util.List;
import java.util.Map;

import static com.virjar.dungproxy.server.proxyservice.common.ProxyResponse.FEEDBACK_SUCCESS_RESPONSE;

/**
 * Description: ProxyReturnHandler
 *
 * @author lingtong.fu
 * @version 2016-10-19 18:19
 */
public class ProxyReturnHandler extends EndpointHandler {

    public static ProxyReturnHandler getInstance() {
        return InstanceHolder.instance;
    }
    private static class InstanceHolder {
        public static final ProxyReturnHandler instance = new ProxyReturnHandler();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final String uri = ((HttpRequest) msg).getUri();
        NetworkUtil.releaseMsgCompletely(msg);
        Channel cc = ctx.channel();
        Map<String, List<String>> paramMap = getRequestParam(uri);

        //TODO 域名 代理检测  反馈降分接口
        NetworkUtil.writeAndFlushAndClose(cc, FEEDBACK_SUCCESS_RESPONSE);
    }
}
