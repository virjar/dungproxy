package com.virjar.proxyservice.common.util;

import com.virjar.proxyservice.common.ProxyResponse;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.ReferenceCounted;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Description: NetworkUtil
 *
 * @author lingtong.fu
 * @version 2016-10-18 16:53
 */
public class NetworkUtil {

    public static void closeChannel(Channel... channels) {
        for (Channel ch : channels) {
            if (ch != null) {
                ch.close();
            }
        }
    }

    /**
     * 检查端口是否被占用
     */
    public static boolean isPortAvailable(int port) {
        if (checkPort(port)) {
            try {
                bindPort("0.0.0.0", port);
                bindPort(InetAddress.getLocalHost().getHostAddress(), port);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private static boolean checkPort(int port) {
        return (port > 0 && port < 65535);
    }

    private static void bindPort(String host, int port) throws Exception {
        Socket s = new Socket();
        s.bind(new InetSocketAddress(host, port));
        s.close();
    }

    public static void writeAndFlushAndClose(Channel channel, FullHttpResponse response) {
        NetworkUtil.resetHandler(channel.pipeline(), new HttpResponseEncoder());
        channel.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    public static void resetHandler(ChannelPipeline pipeline, ChannelHandler handler) {
        ChannelHandler handler1;
        if ((handler1 = pipeline.get(handler.getClass())) != null) {
            pipeline.remove(handler1);
        }
        pipeline.addLast(handler);
    }

    public static void releaseMsgCompletely(Object msg) {
        if (msg != null && msg instanceof ReferenceCounted) {
            ReferenceCounted rc = (ReferenceCounted) msg;
            if (rc.refCnt() > 0) {
                rc.release(rc.refCnt());
            }
        }
    }

    public static void removeHandler(ChannelPipeline p, Class<? extends ChannelHandler> clazz) {
        if (p.get(clazz) != null) {
            p.remove(clazz);
        }
    }

    public static void addHandlerIfAbsent(ChannelPipeline pipeline, ChannelHandler handler) {
        if (pipeline.get(handler.getClass()) == null) {
            pipeline.addLast(handler);
        }
    }
}
