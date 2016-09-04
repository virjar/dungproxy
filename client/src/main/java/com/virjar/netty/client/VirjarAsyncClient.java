package com.virjar.netty.client;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig;
import com.virjar.concurrent.ManagedExecutors;
import com.virjar.concurrent.NamedThreadFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.util.HashedWheelTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sun.deploy.Environment.setUserAgent;

import com.ning.http.client.AsyncHttpClientConfig.Builder;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Description: AsyncClient
 *
 * @author lingtong.fu
 * @version 2016-09-04 11:24
 */
public class VirjarAsyncClient {
    static {
        check();
    }

    public VirjarAsyncClient() {
        shareExecutor = true;

        setCompressionEnabled(true);

        // pooling conf
        setAllowPoolingConnection(true);
        setIdleConnectionInPoolTimeoutInMs(60000);

        // connection conf
        setMaximumConnectionsTotal(100);
        setMaximumConnectionsPerHost(20);

        // request conf
        setConnectionTimeoutInMs(1000);
        setRequestTimeoutInMs(60000);
        setUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64) Chrome/27.0.1453.94 Safari/537.36 hc/8.0.1");
    }

    public void setCompressionEnabled(boolean compressionEnabled) {
        builder.setCompressionEnforced(compressionEnabled);
    }

    public void setAllowPoolingConnection(boolean allowPoolingConnection) {
        builder.setAllowPoolingConnections(allowPoolingConnection);
    }

    public void setIdleConnectionInPoolTimeoutInMs(int defaultIdleConnectionInPoolTimeoutInMs) {
        builder.setPooledConnectionIdleTimeout(defaultIdleConnectionInPoolTimeoutInMs);
    }

    public void setMaximumConnectionsTotal(int defaultMaxTotalConnections) {
        builder.setMaxConnections(defaultMaxTotalConnections);
    }

    public void setMaximumConnectionsPerHost(int defaultMaxConnectionPerHost) {
        builder.setMaxConnectionsPerHost(defaultMaxConnectionPerHost);
    }

    public void setConnectionTimeoutInMs(int defaultConnectionTimeOutInMs) {
        builder.setConnectTimeout(defaultConnectionTimeOutInMs);
    }

    public void setRequestTimeoutInMs(int defaultRequestTimeoutInMs) {
        builder.setRequestTimeout(defaultRequestTimeoutInMs);
    }

    private static final Logger log = LoggerFactory.getLogger(VirjarAsyncClient.class);

    private static void check() {
        try {
            Class.forName("org.jboss.netty.bootstrap.ClientBootstrap");
        } catch (ClassNotFoundException e) {
            String message = "使用AsyncClient必须引入io.netty:netty all in one包.";
            log.error(message);
            throw new RuntimeException(message);
        }
    }

    private static final Supplier<HashedWheelTimer> TIMER = Suppliers.memoize(new Supplier<HashedWheelTimer>() {
        @Override
        public HashedWheelTimer get() {
            HashedWheelTimer timer = new HashedWheelTimer();
            timer.start();
            return timer;
        }
    });

    private static final Supplier<NioClientSocketChannelFactory> CHANNEL_FACTORY = Suppliers.memoize(new Supplier<NioClientSocketChannelFactory>() {
        @Override
        public NioClientSocketChannelFactory get() {
            return new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool(), Runtime.getRuntime().availableProcessors());
        }
    });

    private final Supplier<AsyncHttpClient> lazyClient = Suppliers.memoize(new Supplier<AsyncHttpClient>() {

        @Override
        public AsyncHttpClient get() {
            builder.setExecutorService(ManagedExecutors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2, fact));
            NettyAsyncHttpProviderConfig providerConfig = new NettyAsyncHttpProviderConfig();
            providerConfig.setNettyTimer(TIMER.get());
            if (shareExecutor) {
                providerConfig.setBossExecutorService(ManagedExecutors.getExecutor());
                providerConfig.setSocketChannelFactory(CHANNEL_FACTORY.get());
            }
            builder.setAsyncHttpClientProviderConfig(providerConfig);
            return new AsyncHttpClient(builder.build());
        }
    });

    private boolean shareExecutor;

    private final Builder builder = new Builder();

    private static final ThreadFactory fact = new NamedThreadFactory("hc-nio");

    public AsyncHttpClient getClient() {
        return lazyClient.get();
    }
}
