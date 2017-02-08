package com.virjar.dungproxy.client.ningclient.conn;

import java.lang.reflect.InvocationTargetException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.*;
import com.ning.http.client.providers.jdk.JDKAsyncHttpProvider;
import com.ning.http.client.uri.Uri;
import com.virjar.dungproxy.client.ippool.IpPool;
import com.virjar.dungproxy.client.ippool.IpPoolHolder;
import com.virjar.dungproxy.client.model.AvProxy;

/**
 * Created by virjar on 17/1/31. <br/>
 * 注入本类即可实现IP绑定和IP上下线
 */
public class DungProxyAsyncHttpProvider implements AsyncHttpProvider {
    private static final Logger logger = LoggerFactory.getLogger(DungProxyAsyncHttpProvider.class);
    private final static String DEFAULT_PROVIDER = "com.ning.http.client.providers.netty.NettyAsyncHttpProvider";
    private AsyncHttpProvider delegate;

    private IpPool ipPool;

    /**
     * 这种情况必须引入netty
     */
    public DungProxyAsyncHttpProvider() {
        this(null, null);
    }

    public DungProxyAsyncHttpProvider(AsyncHttpProvider asyncHttpProvider, AsyncHttpClientConfig config) {
        if (asyncHttpProvider == null) {
            asyncHttpProvider = loadDefaultProvider(DEFAULT_PROVIDER, config);
        }
        this.delegate = asyncHttpProvider;
        ipPool = IpPoolHolder.getIpPool();// TODO 这里是否需要扩展
    }

    public DungProxyAsyncHttpProvider(AsyncHttpProvider asyncHttpProvider) {
        this(asyncHttpProvider, null);
    }

    @Override
    public <T> ListenableFuture<T> execute(Request request, AsyncHandler<T> handler) {
        Uri uri = request.getUri();
        if (request.getProxyServer() == null) {// 在这里绑定IP池
            AvProxy proxy = ipPool.bind(uri.getHost(), uri.toUrl());
            if (proxy != null) {
                return delegate.execute(new ExceptionListenRequest(request, proxy),
                        new ExceptionListenHandler<>(proxy, handler));
            }
        }
        return delegate.execute(request, handler);
    }

    @Override
    public void close() {
        delegate.close();
    }

    @SuppressWarnings("unchecked")
    private final static AsyncHttpProvider loadDefaultProvider(String className, AsyncHttpClientConfig config) {
        try {
            Class<AsyncHttpProvider> providerClass = (Class<AsyncHttpProvider>) Thread.currentThread()
                    .getContextClassLoader().loadClass(className);
            return providerClass.getDeclaredConstructor(new Class[] { AsyncHttpClientConfig.class })
                    .newInstance(new Object[] { config });
        } catch (Throwable t) {

            if (t instanceof InvocationTargetException) {
                final InvocationTargetException ite = (InvocationTargetException) t;
                if (logger.isErrorEnabled()) {
                    logger.error("Unable to instantiate provider {}.  Trying other providers.", className);
                    logger.error(ite.getCause().toString(), ite.getCause());
                }
            }

            // Let's try with another classloader
            try {
                Class<AsyncHttpProvider> providerClass = (Class<AsyncHttpProvider>) AsyncHttpClient.class
                        .getClassLoader().loadClass(className);
                return providerClass.getDeclaredConstructor(new Class[] { AsyncHttpClientConfig.class })
                        .newInstance(new Object[] { config });
            } catch (Throwable t2) {
            }

            if (logger.isDebugEnabled()) {
                logger.debug("Default provider not found {}. Using the {}", DEFAULT_PROVIDER,
                        JDKAsyncHttpProvider.class.getName());
            }
            if (config != null) {
                return new JDKAsyncHttpProvider(config);
            }
            throw new IllegalStateException("不支持自动产生JDKAsyncHttpProvider 请手动注入");// 应该不会发生
            // return new JDKAsyncHttpProvider(config);
        }
    }
}
