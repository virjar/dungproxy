package com.virjar.common.util;

import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Created by lingtong.fu
 */
public class IpUtil {

    private static String patternString = "^([1-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3}$";
    private static Pattern pattern = Pattern.compile(patternString);

    private static final Logger log = LoggerFactory.getLogger(IpUtil.class);

    /**
     * 获取客户端ip
     *
     * @param request
     * @return
     */
    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("x-forwarded-for");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        } else {
            /*
             * 当有多级反向代理时，x-forwarded-for值为多个 如: X-Forwarded-For：192.168.1.110， 192.168.1.120， 192.168.1.130 用户真实IP为：
             * 192.168.1.130
             */
            final String ipSeparator = ",";
            if (ip.contains(ipSeparator)) {
                ip = ip.substring(ip.lastIndexOf(ipSeparator), ip.length() - 1);
            }
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            // ip = request.getRemoteAddr();
            /**
             *
             * linux与win运行环境不同
             */
            try {
                Enumeration<?> e1 = NetworkInterface.getNetworkInterfaces();
                while (e1.hasMoreElements()) {
                    NetworkInterface ni = (NetworkInterface) e1.nextElement();
                    if (!ni.getName().equals("eth0")) {
                        continue;
                    } else {
                        Enumeration<?> e2 = ni.getInetAddresses();
                        while (e2.hasMoreElements()) {
                            InetAddress ia = (InetAddress) e2.nextElement();
                            if (ia instanceof Inet6Address)
                                continue;
                            ip = ia.getHostAddress();
                        }
                        break;
                    }
                }
            } catch (SocketException e) {
                log.debug("Error when getting host ip address: <{}>.", e.getMessage());
            }
        }
        return ip;
    }

    public static long getClientIp(String ip) {
        return ipToLong(ip);
    }

    public static String getClientIp() {
        InetAddress addr = null;
        try {
            addr = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            log.error("[getClientIp]fail,host config error", e);
            return null;
        }
        byte[] ipAddr = addr.getAddress();
        String ipAddrStr = "";
        for (int i = 0; i < ipAddr.length; i++) {
            if (i > 0) {
                ipAddrStr += ".";
            }
            ipAddrStr += ipAddr[i] & 0xFF;
        }
        return ipAddrStr;
    }

    /**
     * 获取服务器本地ip
     *
     * @return
     */
    private static List<InetAddress> getAllHostAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            List<InetAddress> addresses = new ArrayList<InetAddress>();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    addresses.add(inetAddress);
                }
            }

            return addresses;
        } catch (SocketException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static List<String> getAllNoLoopbackAddresses() {
        List<String> noLoopbackAddresses = new ArrayList<String>();
        List<InetAddress> allInetAddresses = getAllHostAddress();
        for (InetAddress address : allInetAddresses) {
            if (!address.isLoopbackAddress()) {
                noLoopbackAddresses.add(address.getHostAddress());
            }
        }

        return noLoopbackAddresses;
    }

    public static String getFirstNoLoopbackAddress() {
        List<String> allNoLoopbackAddresses = getAllNoLoopbackAddresses();
        return allNoLoopbackAddresses.iterator().next();
    }

    public static long ipToLong(String ipString) {
        if (StringUtils.isBlank(ipString)) {
            return 0;
        }
        int zeroValue = '0';
        int value = 0;
        long ip = 0L;
        for (int i = 0, n = ipString.length(); i < n; i++) {
            char c = ipString.charAt(i);
            if (c == '.') {
                ip <<= 8;
                ip |= value;
                value = 0;
            } else if (c < '0' || c > '9') {
                return 0L;
            } else {
                value = 10 * value + (c - zeroValue);// 这个处理很高效
            }
        }
        ip <<= 8;
        ip |= value;
        return ip;
    }

    /**
     * 匹配网段
     *
     * @param subnet
     * @param ip
     * @return
     */
    public static boolean netMatch(String subnet, String ip) {
        if (subnet == null || ip == null) {
            return false;
        }
        String[] ipinfo = subnet.split("/");
        if (ipinfo.length != 2) {
            return false;
        }
        String net = ipinfo[0];
        try {
            int mask = Integer.valueOf(ipinfo[1]);
            return (IpUtil.ipToLong(ip) & ~((1 << (32 - mask)) - 1)) == IpUtil.ipToLong(net);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String longToIP(long ip) {
        int[] b = new int[4];
        b[0] = (int) ((ip >> 24) & 0xff);
        b[1] = (int) ((ip >> 16) & 0xff);
        b[2] = (int) ((ip >> 8) & 0xff);
        b[3] = (int) (ip & 0xff);
        StringBuilder sb = new StringBuilder("");
        sb.append(Integer.toString(b[0]));
        sb.append(".");
        sb.append(Integer.toString(b[1]));
        sb.append(".");
        sb.append(Integer.toString(b[2]));
        sb.append(".");
        sb.append(Integer.toString(b[3]));
        return sb.toString();
    }

    public static boolean isIPAddress(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.find();
    }
}
