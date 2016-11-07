/*
 * Copyright (c) 2010-2012 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package com.virjar.dungproxy.server.proxyservice.client.listener;

import com.google.common.base.Preconditions;
import io.netty.channel.Channel;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.Timer;
import org.jboss.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class SimpleConnectionsPool {

    private final static Logger log = LoggerFactory.getLogger(SimpleConnectionsPool.class);
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<IdleChannel>> connectionsPool = new ConcurrentHashMap<String, ConcurrentLinkedQueue<IdleChannel>>();
    private final ConcurrentHashMap<Channel, IdleChannel> channel2IdleChannel = new ConcurrentHashMap<Channel, IdleChannel>();
    private final ConcurrentHashMap<Channel, Long> channel2CreationDate = new ConcurrentHashMap<Channel, Long>();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private final Timer nettyTimer;
    private final boolean sslConnectionPoolEnabled;
    private final int maxTotalConnections;
    private final int maxConnectionPerHost;
    private final int maxConnectionLifeTimeInMs;
    private final long maxIdleTime;

    public SimpleConnectionsPool(int maxTotalConnections, int maxConnectionPerHost, long maxIdleTime, int maxConnectionLifeTimeInMs, boolean sslConnectionPoolEnabled,
                                 Timer nettyTimer) {
        Preconditions.checkArgument(maxConnectionLifeTimeInMs == -1, "暂不支持控制channel最大存活时间, 只能设置maxConnectionLifeTimeInMs = -1");
        //TODO 目前这个功能在channel被poll走了以后不用offer归还时，会使channel2CreationDate中entry不释放，导致内存泄漏，在需要这个功能时再支持它
        this.maxTotalConnections = maxTotalConnections;
        this.maxConnectionPerHost = maxConnectionPerHost;
        this.sslConnectionPoolEnabled = sslConnectionPoolEnabled;
        this.maxIdleTime = maxIdleTime;
        this.maxConnectionLifeTimeInMs = maxConnectionLifeTimeInMs;
        this.nettyTimer = nettyTimer;
        scheduleNewIdleChannelDetector(new IdleChannelDetector());
    }

    private void scheduleNewIdleChannelDetector(TimerTask task) {
        this.nettyTimer.newTimeout(task, maxIdleTime, TimeUnit.MILLISECONDS);
    }

    private static class IdleChannel {
        final String uri;
        final Channel channel;
        final long start;

        IdleChannel(String uri, Channel channel) {
            this.uri = uri;
            this.channel = channel;
            this.start = millisTime();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof IdleChannel))
                return false;

            IdleChannel that = (IdleChannel) o;

            return !(channel != null ? !channel.equals(that.channel) : that.channel != null);

        }

        @Override
        public int hashCode() {
            return channel != null ? channel.hashCode() : 0;
        }
    }

    private class IdleChannelDetector implements TimerTask {

        public void run(Timeout timeout) throws Exception {
            try {
                if (isClosed.get())
                    return;

                Set<String> keys = connectionsPool.keySet();
                for (String s : keys) {
                    int count = connectionsPool.get(s).size();
                    log.debug("Entry count for : {} : {}", s, count);
                }

                List<IdleChannel> channelsInTimeout = new ArrayList<IdleChannel>();
                long currentTime = millisTime();

                for (IdleChannel idleChannel : channel2IdleChannel.values()) {
                    long age = currentTime - idleChannel.start;
                    if (age > maxIdleTime) {

                        log.debug("Adding Candidate Idle Channel {}", idleChannel.channel);

                        // store in an unsynchronized list to minimize the impact on the ConcurrentHashMap.
                        channelsInTimeout.add(idleChannel);
                    }
                }
                long endConcurrentLoop = millisTime();

                for (IdleChannel idleChannel : channelsInTimeout) {
                    if (remove(idleChannel)) {
                        log.debug("Closing Idle Channel {}", idleChannel.channel);
                        close(idleChannel.channel);
                    }
                }

                if (log.isTraceEnabled()) {
                    int openChannels = 0;
                    for (ConcurrentLinkedQueue<IdleChannel> hostChannels : connectionsPool.values()) {
                        openChannels += hostChannels.size();
                    }
                    log.trace(String.format("%d channel open, %d idle channels closed (times: 1st-loop=%d, 2nd-loop=%d).\n", openChannels, channelsInTimeout.size(),
                            endConcurrentLoop - currentTime, millisTime() - endConcurrentLoop));
                }

            } catch (Throwable t) {
                log.error("uncaught exception!", t);
            }

            scheduleNewIdleChannelDetector(timeout.getTask());
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean offer(String uri, Channel channel) {
        if (isClosed.get())
            return false;

        if (!sslConnectionPoolEnabled && uri.startsWith("https")) {
            return false;
        }

        if (maxConnectionLifeTimeInMs != -1) {
            Long createTime = channel2CreationDate.get(channel);
            if (createTime == null) {
                channel2CreationDate.putIfAbsent(channel, millisTime());
            } else if (createTime + maxConnectionLifeTimeInMs < millisTime()) {
                log.debug("Channel {} expired", channel);
                return false;
            }
        }

        log.debug("Adding uri: {} for channel {}", uri, channel);

        ConcurrentLinkedQueue<IdleChannel> idleConnectionForHost = connectionsPool.get(uri);
        if (idleConnectionForHost == null) {
            ConcurrentLinkedQueue<IdleChannel> newPool = new ConcurrentLinkedQueue<IdleChannel>();
            idleConnectionForHost = connectionsPool.putIfAbsent(uri, newPool);
            if (idleConnectionForHost == null)
                idleConnectionForHost = newPool;
        }

        boolean added;
        int size = idleConnectionForHost.size();
        if (maxConnectionPerHost == -1 || size < maxConnectionPerHost) {
            IdleChannel idleChannel = new IdleChannel(uri, channel);
            synchronized (idleConnectionForHost) {
                added = idleConnectionForHost.add(idleChannel);

                if (channel2IdleChannel.put(channel, idleChannel) != null) {
                    log.error("Channel {} already exists in the connections pool!", channel);
                }
            }
        } else {
            log.debug("Maximum number of requests per host reached {} for {}", maxConnectionPerHost, uri);
            added = false;
        }
        if (added) {
            log.debug("offer connection, uri: {}", uri);
        }
        if (added) {
            // 开启 autoRead, 这样能及时接收到 channel 状态变更
            channel.config().setAutoRead(true);
        }
        return added;
    }

    /**
     * {@inheritDoc}
     */
    public Channel poll(String uri) {
        log.debug("poll connection, uri: {}", uri);
        if (!sslConnectionPoolEnabled && uri.startsWith("https")) {
            return null;
        }

        IdleChannel idleChannel = null;
        ConcurrentLinkedQueue<IdleChannel> idleConnectionForHost = connectionsPool.get(uri);
        if (idleConnectionForHost != null) {
            boolean poolEmpty = false;
            while (!poolEmpty && idleChannel == null) {
                if (!idleConnectionForHost.isEmpty()) {
                    synchronized (idleConnectionForHost) {
                        idleChannel = idleConnectionForHost.poll();
                        if (idleChannel != null) {
                            channel2IdleChannel.remove(idleChannel.channel);
                        }
                    }
                }

                if (idleChannel == null) {
                    poolEmpty = true;
                } else if (!idleChannel.channel.isActive()) {
                    idleChannel = null;
                    log.debug("Channel not connected or not opened!");
                }
            }
        }
        return idleChannel != null ? idleChannel.channel : null;
    }

    private boolean remove(IdleChannel pooledChannel) {
        if (pooledChannel == null || isClosed.get())
            return false;

        boolean isRemoved = false;
        ConcurrentLinkedQueue<IdleChannel> pooledConnectionForHost = connectionsPool.get(pooledChannel.uri);
        if (pooledConnectionForHost != null) {
            isRemoved = pooledConnectionForHost.remove(pooledChannel);
        }
        isRemoved |= channel2IdleChannel.remove(pooledChannel.channel) != null;
        return isRemoved;
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeAll(Channel channel) {
        channel2CreationDate.remove(channel);
        return !isClosed.get() && remove(channel2IdleChannel.get(channel));
    }

    /**
     * {@inheritDoc}
     */
    public boolean canCacheConnection() {
        return !(!isClosed.get() && maxTotalConnections != -1 && channel2IdleChannel.size() >= maxTotalConnections);
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
        if (isClosed.getAndSet(true))
            return;

        for (Channel channel : channel2IdleChannel.keySet()) {
            close(channel);
        }
        connectionsPool.clear();
        channel2IdleChannel.clear();
        channel2CreationDate.clear();
    }

    private void close(Channel channel) {
        try {
            channel2CreationDate.remove(channel);
            channel.close();
        } catch (Throwable t) {
            log.error("Close channel failed.", t);
            // noop
        }
    }

    public final String toString() {
        return String.format("NettyConnectionPool: {pool-size: %d}", channel2IdleChannel.size());
    }

    private static long millisTime() {
        return System.currentTimeMillis();
    }
}
