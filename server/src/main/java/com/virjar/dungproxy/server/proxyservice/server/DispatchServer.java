package com.virjar.dungproxy.server.proxyservice.server;

import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.server.entity.Proxy;
import com.virjar.dungproxy.server.proxyservice.common.util.NetworkUtil;
import com.virjar.dungproxy.server.proxyservice.handler.DispatchHandler;
import com.virjar.dungproxy.server.proxyservice.handler.DispatchHandlerInitializer;
import com.virjar.dungproxy.server.repository.ProxyRepository;
import com.virjar.dungproxy.server.utils.SysConfig;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.BasicHttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import com.virjar.dungproxy.server.proxyservice.common.util.Executors;

import javax.annotation.Resource;

import static com.virjar.dungproxy.server.proxyservice.common.util.Executors.bossGroup;
import static com.virjar.dungproxy.server.proxyservice.common.util.Executors.workerGroup;

/**
 * Description: DispatchServer
 *
 * @author lingtong.fu
 * @version 2016-10-18 17:00
 */
public class DispatchServer extends Thread {

    @Resource
    private ProxyRepository proxyRepo;

    private static final Logger LOGGER = LoggerFactory.getLogger(DispatchServer.class);

    private NioServerSocketChannel ch;


    private int serverPort;

    private String serverHost;

    public DispatchServer(int serverPort, String serverHost) {
        this.serverPort = serverPort;
        this.serverHost = serverHost;
    }

    @Override
    public void run() {

        // 端口检测
        for (int port = 8081; port <= 9090; port++) {
            if (NetworkUtil.isPortAvailable(port)) {
                serverPort = port;
                break;
            }
        }

        if (serverPort == 0) {
            LOGGER.error("无法找到可用端口");
            return;
        }

        // TODO 加载 domain和proxy数据
        List<Proxy> proxyList = proxyRepo.findAvailable();

        try {
            // TEST
            HttpClientContext httpClientContext = HttpClientContext.adapt(new BasicHttpContext());
            String quatity = HttpInvoker.get("http://www.66ip.cn/3.html", httpClientContext);

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
                                    new DispatchHandler(serverHost))
                    );

            ch = (NioServerSocketChannel) b.bind(serverPort).addListener(new ChannelFutureListener() {

                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        LOGGER.info("Netty 服务器成功启动, 端口 {}", serverPort);
                    } else {
                        LOGGER.error("Netty 启动失败, port [{}] 原因: ", serverPort, future.cause());
                    }
                }
            }).sync().channel();
            ch.closeFuture().sync();
        } catch (Exception e) {
            LOGGER.error("***: Netty 服务器启动失败, 端口 {}", serverPort, e);
        }
    }

    /**
     * 进程结束时,清理资源
     */
    public void shutdown() {
        try {
            LOGGER.info("准备关闭Netty服务器, 端口 {}", serverPort);
            // 阻塞, 等待断开连接和关闭操作完成
            if (ch != null && ch.isActive()) {
                ch.close().awaitUninterruptibly();
            }
            Executors.shutdownAll();
            //TODO proxyClient close
            LOGGER.info("成功关闭Netty服务器, 端口 {}", serverPort);
        } catch (Exception e) {
            LOGGER.warn("关闭Netty服务器时出错, 端口 {}", serverPort, e);
        }
    }
}
