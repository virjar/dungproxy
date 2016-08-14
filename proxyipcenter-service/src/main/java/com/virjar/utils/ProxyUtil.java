package com.virjar.utils;

import java.net.*;
import java.util.Enumeration;
import java.util.List;

import com.google.common.base.Splitter;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.virjar.model.ProxyModel;
import com.virjar.utils.net.HttpInvoker;
import com.virjar.utils.net.HttpResult;

/**
 * ClassName:ProxyUtil
 * 
 * @see
 * @author ch
 * @version Ver 1.0
 * @Date 2014-2-16 下午04:20:07
 */
public class ProxyUtil {
    public final static String testkey = "if a client get this string,we think proxy is realy avaible. qq group 428174328";
    private static final String keysourceurl = SysConfig.getInstance().getKeyverifyurl();
    private static InetAddress localAddr;
    private static final Logger logger = LoggerFactory.getLogger(ProxyUtil.class);
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

    public static boolean validateProxyAvailable(ProxyModel p) {
        HttpResult request = null;
        try {
            request = new HttpInvoker(keysourceurl).setproxy(p.getIp(), p.getPort()).request();
            if (request != null) {
                String key = new String(request.getResponseBody());
                if (key != null && key.contains("428174328")) {
                    return true;
                }
            }
        } catch (Exception e) {
        }

        return false;
    }

    public static boolean validateProxyConnect(HttpHost p) {
        if (localAddr == null) {
            logger.error("cannot get local ip");
            throw new IllegalStateException("cannot get local ip");
        }
        Socket socket = null;
        try {
            socket = new Socket();
            socket.bind(new InetSocketAddress(localAddr, 0));
            InetSocketAddress endpointSocketAddr = new InetSocketAddress(p.getAddress().getHostAddress(), p.getPort());
            socket.connect(endpointSocketAddr, 3000);
            logger.debug("SUCCESS - connection established! Local: " + localAddr.getHostAddress() + " remote: " + p);
            return true;
        } catch (Exception e) {
            // 日志级别为debug,失败的IP数量非常多
            logger.debug("FAILRE - CAN not connect! Local: " + localAddr.getHostAddress() + " remote: " + p);
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
    public static Long toIPValue(String ipAddress) {
        List<String> strings = Splitter.on(".").trimResults().splitToList(ipAddress);
        Long ret = 0L;
        ret |= Long.parseLong(strings.get(0)) << 24;
        ret |= Long.parseLong(strings.get(1)) << 16;
        ret |= Long.parseLong(strings.get(2)) << 8;
        ret |= Long.parseLong(strings.get(3));
        return ret;
    }
}
