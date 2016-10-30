package com.virjar.common.cache;

import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.virjar.dungproxy.client.model.Proxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;

/**
 * Description: Proxies
 *
 * @author lingtong.fu
 * @version 2016-09-05 21:36
 */
public class Proxies {
    private static final Logger LOGGER = LoggerFactory.getLogger(Proxies.class);
    /**
     * id与Proxy映射, id => proxy
     */
    private static BiMap<Long, Proxy> idProxyMap = Maps.synchronizedBiMap(HashBiMap.<Long, Proxy>create());

    /**
     * 将代理按tag分组
     */
    private static Map<Group, ArrayListMultimap<String, Proxy>> groupProxyMap = Maps.newHashMap();

    private static ImmutableList<ProxyOutput> proxyOutputs = ImmutableList.of();

    static {
        for (Group tag : Group.values()) {
            groupProxyMap.put(tag, ArrayListMultimap.<String, Proxy>create());
        }
    }

    private volatile static ImmutableMap<Proxy.Tag, ImmutableList<Proxy>> tagProxyMap = ImmutableMap.of();

    private static final Set<DomainProxyPair> NON_EXIST_ID_SET = Sets.newConcurrentHashSet();

    private static final ProxyFilter FILTER_CHAIN;

    /**
     * 通过Id获取proxy
     */
    public static Proxy getProxy(long proxyId) {
        Proxy p = idProxyMap.get(proxyId);
        if (p == null || !p.isAvailable()) {
            return null;
        }
        return p;
    }


    public static class ProxyOutput {
        private final long id;
        private final String type;

        public ProxyOutput(long id, String type) {
            this.id = id;
            this.type = type;
        }

        public long getId() {
            return id;
        }

        public String getType() {
            return type;
        }
    }

    public static class DomainProxyPair {
        private int domainId;
        private long proxyId;

        public DomainProxyPair(int doaminId, long proxyId) {
            this.domainId = doaminId;
            this.proxyId = proxyId;
        }

        public int getDomainId() {
            return domainId;
        }

        public long getProxyId() {
            return proxyId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            DomainProxyPair that = (DomainProxyPair) o;

            if (domainId != that.domainId) return false;
            if (proxyId != that.proxyId) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = domainId;
            result = 31 * result + (int) (proxyId ^ (proxyId >>> 32));
            return result;
        }
    }

    static {
        ProxyFilter allFilter = new ProxyFilter(Group.ALL) {

            @Override
            public boolean apply(Proxy proxy) {
                return !isFixedIp(proxy);
            }
        };

        ProxyFilter allHttpsFilter = new ProxyFilter(Group.ALL_HTTPS) {
            @Override
            public boolean apply(Proxy proxy) {
                return !isFixedIp(proxy) && (proxy.getHttps() == 1);
            }
        };

        ProxyFilter allFreeFilter = new ProxyFilter(Group.FREE_ALL) {
            @Override
            public boolean apply(Proxy proxy) {
                return !isFixedIp(proxy) && proxy.isFree();
            }
        };

        ProxyFilter freeHttpsFilter = new ProxyFilter(Group.FREE_HTTPS) {
            @Override
            public boolean apply(Proxy proxy) {
                return !isFixedIp(proxy) && proxy.isFree() && (proxy.getHttps() == 1);
            }
        };

        ProxyFilter noneFreeHttpsFilter = new ProxyFilter(Group.NON_FREE_HTTPS) {
            @Override
            public boolean apply(Proxy proxy) {
                return !isFixedIp(proxy) && !proxy.isFree() && (proxy.getHttps() == 1);
            }
        };

        FILTER_CHAIN = allFilter
                .chain(allHttpsFilter)
                .chain(allFreeFilter)
                .chain(freeHttpsFilter)
                .chain(noneFreeHttpsFilter);
    }

    public static boolean isFixedIp(Proxy proxy) {
        return proxy.getTag() == Proxy.Tag.FIXED_IP_FLI  || proxy.getTag() == Proxy.Tag.SOCKS5_NOAUTH;
    }

    /**
     * tags
     */
    public enum Group {
        ALL, ALL_HTTPS, FREE_ALL, FREE_HTTPS, NON_FREE_HTTPS
    }

    private static abstract class ProxyFilter {
        private ProxyFilter next;
        private Group tag;

        protected ProxyFilter(Group tag) {
            this.tag = tag;
        }

        public ProxyFilter chain(ProxyFilter head) {
            head.next = this;
            return head;
        }

        public void filter(Proxy proxy, Map<Group, ArrayListMultimap<String, Proxy>> map) {
            ProxyFilter filter = this;
            do {
                ArrayListMultimap<String, Proxy> multimap = map.get(filter.tag);
                if (multimap != null) {
                    if (filter.apply(proxy)) {
                        if (Strings.isNullOrEmpty(proxy.getCity())) {
                            multimap.put("UNKNOWN", proxy);
                        } else {
                            multimap.put(proxy.getCity(), proxy);
                        }
                    }
                }
                filter = filter.next;
            } while (filter != null);
        }

        public abstract boolean apply(Proxy proxy);
    }


}
