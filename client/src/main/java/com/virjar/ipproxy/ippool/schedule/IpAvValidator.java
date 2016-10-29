package com.virjar.ipproxy.ippool.schedule;

import java.io.IOException;
import java.net.*;
import java.util.Enumeration;

import com.virjar.ipproxy.httpclient.HttpInvoker;
import com.virjar.model.AvProxy;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Description: 本地可用Proxy验证
 *
 * @author lingtong.fu
 * @version 2016-09-16 16:57
 */
public class IpAvValidator {
    private static final Logger logger = LoggerFactory.getLogger(IpAvValidator.class);
    private static InetAddress localAddr;
    static {
        init();
    }

    private static void init() {
        Enumeration<InetAddress> localAddrs;
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface ni = networkInterfaces.nextElement();
                localAddrs = ni.getInetAddresses();
                while (localAddrs.hasMoreElements()) {
                    InetAddress tmp = localAddrs.nextElement();
                    if (!tmp.isLoopbackAddress() && !tmp.isLinkLocalAddress() && !(tmp instanceof Inet6Address)) {
                        localAddr = tmp;
                        logger.info("local IP:" + localAddr.getHostAddress());
                        return;
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Failure when init ProxyUtil", e);
            logger.error("choose NetworkInterface\n" + getNetworkInterface());
        }

    }
    public static boolean available(AvProxy avProxy, String testUrl) {
        for (int i = 0; i < 3; i++) {
            try {
                if (HttpInvoker.getStatus(testUrl, avProxy.getIp(), avProxy.getPort()) == 200) {
                    return true;
                }
            } catch (IOException e) {
                // do nothing
            }
        }
        return false;
    }


    private static Socket newLocalSocket() {
        for (int i = 0; i < 3; i++) {
            Socket socket = new Socket();
            try {
                socket.bind(new InetSocketAddress(localAddr, 0));
                return socket;
            } catch (IOException e) {
                logger.warn("系统资源不足,本地端口开启失败");
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return null;
    }

    public static boolean validateProxyConnect(HttpHost p) {
        if (localAddr == null) {
            logger.error("cannot get local ip");
            throw new IllegalStateException("cannot get local ip");
        }
        Socket socket = newLocalSocket();
        if (socket == null) {
            return false;
        }
        try {
            InetSocketAddress endpointSocketAddr = new InetSocketAddress(p.getAddress().getHostAddress(), p.getPort());
            socket.connect(endpointSocketAddr, 4000);
            return true;
        } catch (Exception e) {
            // 日志级别为debug,失败的IP数量非常多
        } finally {
            IOUtils.closeQuietly(socket);
        }
        return false;
    }

    private static String getNetworkInterface() {
        String networkInterfaceName = "";
        Enumeration<NetworkInterface> enumeration = null;
        try {
            enumeration = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e1) {
            e1.printStackTrace();
        }
        while (enumeration.hasMoreElements()) {
            NetworkInterface networkInterface = enumeration.nextElement();
            networkInterfaceName += networkInterface.toString() + '\n';
            Enumeration<InetAddress> addr = networkInterface.getInetAddresses();
            while (addr.hasMoreElements()) {
                networkInterfaceName += "\tip:" + addr.nextElement().getHostAddress() + "\n";
            }
        }
        return networkInterfaceName;
    }
}
