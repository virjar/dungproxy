package com.virjar.utils;

import java.util.Iterator;
import java.util.List;

import com.virjar.entity.Proxy;

public class ResourceFilter {
    // hard code ,because don`t know how to use it
    /*
     * code segment in heritrix int n_expected = (args.length > 1) ? Integer.parseInt(args[1]) : 10000000; int d_hashes
     * = (args.length > 2) ? Integer.parseInt(args[2]) : 22;
     */
    private static BloomFilter bloomFilter = new BloomFilter64bit(10000000, 22);
    private static String ipregex = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";

    public static List<Proxy> filter(List<Proxy> proxys) {
        Iterator<Proxy> iterator = proxys.iterator();
        while (iterator.hasNext()) {
            Proxy proxy = iterator.next();
            if (proxy.getIp() == null || proxy.getPort() == null) {
                iterator.remove();
                continue;
            } else if (bloomFilter.contains(proxy.getIp() + proxy.getPort())) {
                iterator.remove();
                continue;
            } else {
                bloomFilter.add(proxy.getIp() + proxy.getPort());
            }
            if (!proxy.getIp().matches(ipregex)) {
                iterator.remove();
            }
            proxy.setIpValue(ProxyUtil.toIPValue(proxy.getIp()));
        }
        return proxys;
    }

    public static void addConflict(Proxy proxy) {
        bloomFilter.add(proxy.getIp() + proxy.getPort());
    }

    public static boolean contains(Proxy proxy) {
        return bloomFilter.contains(proxy.getIp() + proxy.getPort());
    }

    public static void main(String args[]) {
        System.out.println("221111..71.20.246".matches(ipregex));
        System.out.println("211.71.20.246".matches(ipregex));
        System.out.println("211.71.20.246211.71.20.246".matches(ipregex));
        System.out.println("sdgd".matches(ipregex));
    }

}
