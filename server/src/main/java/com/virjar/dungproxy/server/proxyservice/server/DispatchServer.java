package com.virjar.dungproxy.server.proxyservice.server;

import static com.virjar.dungproxy.server.proxyservice.common.util.Executors.bossGroup;
import static com.virjar.dungproxy.server.proxyservice.common.util.Executors.workerGroup;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.virjar.dungproxy.server.proxyservice.client.NettyHttpClient;
import com.virjar.dungproxy.server.proxyservice.common.util.Executors;
import com.virjar.dungproxy.server.proxyservice.common.util.NetworkUtil;
import com.virjar.dungproxy.server.proxyservice.handler.DispatchHandler;
import com.virjar.dungproxy.server.proxyservice.handler.DispatchHandlerInitializer;
import com.virjar.dungproxy.server.utils.SysConfig;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Description: DispatchServer
 *
 * @author lingtong.fu
 * @version 2016-10-18 17:00
 */
public class DispatchServer {

    private static final Logger log = LoggerFactory.getLogger(DispatchServer.class);

    private int serverPort = 0;

    private String serverHost;

    private NioServerSocketChannel ch;

    private NettyHttpClient nettyHttpClient;

    @Resource
    private ProxySelector proxySelector;

    public DispatchServer(int serverPort, String serverHost) {
        this.serverPort = serverPort;
        this.serverHost = serverHost;
        try {
            this.nettyHttpClient = new NettyHttpClient();
        } catch (Exception e) {
            log.error("Create nettyHttpClient error.", e);
        }
    }

    public void init() {
        if (!NetworkUtil.isPortAvailable(8081)) {
            throw new IllegalStateException("8081端口号不可用, Netty Server启动失败");
        }

        new Thread() {
            @Override
            public void run() {
                try {
                    startNettyServer();
                } catch (InterruptedException e) {
                    log.error(" Netty Server 启动失败, 端口 {}", serverPort, e);
                }
            }
        }.start();

    }

    private void startNettyServer() throws InterruptedException {
        //启动Netty Server 提供统一代理服务转发请求
        ServerBootstrap b = new ServerBootstrap();
        b.option(ChannelOption.SO_REUSEADDR, true);
        b.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        b.childOption(ChannelOption.RCVBUF_ALLOCATOR, AdaptiveRecvByteBufAllocator.DEFAULT);
        b.childOption(ChannelOption.TCP_NODELAY, true);
        b.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, WriteBufferWaterMark.DEFAULT);
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(
                        new DispatchHandlerInitializer(
                                SysConfig.getInstance().getHttpClientCodecMaxInitialLineLength(),
                                SysConfig.getInstance().getHttpClientCodecMaxHeaderSize(),
                                SysConfig.getInstance().getHttpClientCodecMaxChunkSize(),
                                SysConfig.getInstance().getMaxAggregateSize(),
                                SysConfig.getInstance().getClientReadTimeoutSeconds(),
                                SysConfig.getInstance().getClientWriteTimeoutSeconds(),
                                SysConfig.getInstance().getClientAllTimeoutSeconds(),
                                new DispatchHandler(proxySelector, nettyHttpClient))
                );

        ch = (NioServerSocketChannel) b.bind(serverPort).addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    log.info("Netty Server 成功启动, 端口 {}", serverPort);
                } else {
                    log.error("Netty Server 启动失败, 端口 {} 原因: ", serverPort, future.cause());
                }
            }
        }).sync().channel();
        ch.closeFuture().sync();
    }

    /**
     * 进程结束时,清理资源
     */
    public void shutdown() {
        try {
            log.info("准备关闭Netty服务器, 端口 {}", serverPort);
            // 阻塞, 等待断开连接和关闭操作完成
            if (ch != null && ch.isActive()) {
                ch.close().awaitUninterruptibly();
            }
            Executors.shutdownAll();
            log.info("成功关闭Netty服务器, 端口 {}", serverPort);
        } catch (Exception e) {
            log.warn("关闭Netty服务器时出错, 端口 {}", serverPort, e);
        }
    }
}
