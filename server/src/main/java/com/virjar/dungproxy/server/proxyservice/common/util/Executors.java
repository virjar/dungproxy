package com.virjar.dungproxy.server.proxyservice.common.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.nio.NioEventLoopGroup;

/**
 * Description: Executors
 *
 * @author lingtong.fu
 * @version 2016-10-18 17:20
 */
public class Executors {

    public static final int DEFAULT_THREAD_NUM = Runtime.getRuntime().availableProcessors() * 2;

    public static final NioEventLoopGroup bossGroup = new NioEventLoopGroup(Executors.DEFAULT_THREAD_NUM,
            new ThreadFactoryBuilder().setNameFormat("qp-boss-%s").build());

    public static final NioEventLoopGroup workerGroup = new NioEventLoopGroup(Executors.DEFAULT_THREAD_NUM,
            new ThreadFactoryBuilder().setNameFormat("qp-worker-%s").build());

    public static void shutdownAll() {
        if (!bossGroup.isShutdown()) {
            bossGroup.shutdownGracefully().awaitUninterruptibly();
        }
        if (!workerGroup.isShutdown()) {
            workerGroup.shutdownGracefully().awaitUninterruptibly();
        }
    }
}
