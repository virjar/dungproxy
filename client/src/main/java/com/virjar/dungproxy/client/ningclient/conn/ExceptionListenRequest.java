package com.virjar.dungproxy.client.ningclient.conn;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ning.http.client.*;
import com.ning.http.client.cookie.Cookie;
import com.ning.http.client.multipart.Part;
import com.ning.http.client.uri.Uri;
import com.virjar.dungproxy.client.model.AvProxy;

/**
 * Created by virjar on 17/1/31.
 */
public class ExceptionListenRequest implements Request {
    private Logger logger = LoggerFactory.getLogger(ExceptionListenRequest.class);
    private Request delegate;
    private AvProxy avProxy;

    public ExceptionListenRequest(Request delegate, AvProxy avProxy) {
        this.delegate = delegate;
        this.avProxy = avProxy;
    }

    @Override
    public String getBodyEncoding() {
        return delegate.getBodyEncoding();
    }

    @Override
    public BodyGenerator getBodyGenerator() {
        return delegate.getBodyGenerator();
    }

    @Override
    public byte[] getByteData() {
        return delegate.getByteData();
    }

    @Override
    public List<byte[]> getCompositeByteData() {
        return delegate.getCompositeByteData();
    }

    @Override
    public ConnectionPoolPartitioning getConnectionPoolPartitioning() {
        return delegate.getConnectionPoolPartitioning();
    }

    @Override
    public long getContentLength() {
        return delegate.getContentLength();
    }

    @Override
    public Collection<Cookie> getCookies() {
        return delegate.getCookies();
    }

    @Override
    public File getFile() {
        return delegate.getFile();
    }

    @Override
    public Boolean getFollowRedirect() {
        return delegate.getFollowRedirect();
    }

    @Override
    public List<Param> getFormParams() {
        return delegate.getFormParams();
    }

    @Override
    public FluentCaseInsensitiveStringsMap getHeaders() {
        return delegate.getHeaders();
    }

    @Override
    public InetAddress getInetAddress() {
        return delegate.getInetAddress();
    }

    @Override
    public InetAddress getLocalAddress() {
        return delegate.getLocalAddress();
    }

    @Override
    public String getMethod() {
        return delegate.getMethod();
    }

    @Override
    public NameResolver getNameResolver() {
        return delegate.getNameResolver();
    }

    @Override
    public List<Part> getParts() {
        return delegate.getParts();
    }

    @Override
    public ProxyServer getProxyServer() {
        ProxyServer proxyServer = delegate.getProxyServer();
        if (proxyServer != null) {
            return proxyServer;
        }

        ProxyServer.Protocol protocol;
        Uri uri = getUri();
        if (uri.getScheme().equalsIgnoreCase("https")) {
            protocol = ProxyServer.Protocol.HTTPS;
        } else if (uri.getScheme().equalsIgnoreCase("http")) {
            protocol = ProxyServer.Protocol.HTTP;
        } else { // WebSocket的协议不支持被dungproxy代理
            logger.warn("DungProxy 只支持http和https的代理");
            return null;
        }
        return new ProxyServer(protocol, avProxy.getIp(), avProxy.getPort(), avProxy.getUsername(),
                avProxy.getPassword());
    }

    @Override
    public List<Param> getQueryParams() {
        return delegate.getQueryParams();
    }

    @Override
    public long getRangeOffset() {
        return delegate.getRangeOffset();
    }

    @Override
    public Realm getRealm() {
        return delegate.getRealm();
    }

    @Override
    public int getRequestTimeout() {
        return delegate.getRequestTimeout();
    }

    @Override
    public InputStream getStreamData() {
        return delegate.getStreamData();
    }

    @Override
    public String getStringData() {
        return delegate.getStringData();
    }

    @Override
    public Uri getUri() {
        return delegate.getUri();
    }

    @Override
    public String getUrl() {
        return delegate.getUrl();
    }

    @Override
    public String getVirtualHost() {
        return delegate.getVirtualHost();
    }
}
