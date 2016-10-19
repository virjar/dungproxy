package com.virjar.proxyservice.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Description:
 * Sharable: 为每个connection创建独立的 handler实例，来避免产生 race condition .
 * 注:非线程安全.
 *
 * @author lingtong.fu
 * @version 2016-10-18 16:02
 */
@ChannelHandler.Sharable
public class DispatchHandlerInitializer extends ChannelInitializer<SocketChannel> {
    /**
     * 防止内存过度消耗参数.
     */
    private int httpClientCodecMaxInitialLineLength;
    /**
     * headers 最大长度限制.
     */
    private int httpClientCodecMaxHeaderSize;
    /**
     * The maximum length of the content or each chunk.
     */
    private int httpClientCodecMaxChunkSize;
    private int maxAggregateSize;
    private int clientReadTimeoutSeconds;
    private int clientWriteTimeoutSeconds;
    private int clientAllTimeoutSeconds;
    private DispatchHandler dispatchHandler;

    public DispatchHandlerInitializer(
            int httpClientCodecMaxInitialLineLength,
            int httpClientCodecMaxHeaderSize,
            int httpClientCodecMaxChunkSize,
            int maxAggregateSize,
            int clientReadTimeoutSeconds,
            int clientWriteTimeoutSeconds,
            int clientAllTimeoutSeconds,
            DispatchHandler dispatchHandler) {
        this.httpClientCodecMaxInitialLineLength = httpClientCodecMaxInitialLineLength;
        this.httpClientCodecMaxHeaderSize = httpClientCodecMaxHeaderSize;
        this.httpClientCodecMaxChunkSize = httpClientCodecMaxChunkSize;
        this.maxAggregateSize = maxAggregateSize;
        this.clientReadTimeoutSeconds = clientReadTimeoutSeconds;
        this.clientWriteTimeoutSeconds = clientWriteTimeoutSeconds;
        this.clientAllTimeoutSeconds = clientAllTimeoutSeconds;
        this.dispatchHandler = dispatchHandler;
    }

    @Override
    protected void initChannel(SocketChannel sc) throws Exception {
        ChannelPipeline cp = sc.pipeline();
        //处理 clientChannel的关闭, 读超时或写超时向后传递空闲事件.
        cp.addLast(new IdleStateHandler(clientReadTimeoutSeconds, clientWriteTimeoutSeconds, clientAllTimeoutSeconds));
        cp.addLast(TimeoutHandler.instance);
        cp.addLast(new HttpRequestDecoder(httpClientCodecMaxInitialLineLength, httpClientCodecMaxHeaderSize, httpClientCodecMaxChunkSize));
        //把多个消息转换为一个单一的FullHttpRequest或是FullHttpResponse。
        cp.addLast(new HttpObjectAggregator(maxAggregateSize));
        cp.addLast(new HttpResponseEncoder());
        cp.addLast(dispatchHandler);
    }
}
