package com.virjar.dungproxy.server.proxyservice.server;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.virjar.dungproxy.server.entity.DomainIp;
import com.virjar.dungproxy.server.entity.Proxy;
import com.virjar.dungproxy.server.proxyservice.common.util.NetworkUtil;
import com.virjar.dungproxy.server.repository.DomainIpRepository;
import com.virjar.dungproxy.server.repository.ProxyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import javax.annotation.Resource;
import java.util.List;

/**
 * Description: ProxySelectorHolder
 *
 * @author lingtong.fu
 * @version 2016-11-11 14:56
 */
public class ProxySelector {

    private static final Logger log = LoggerFactory.getLogger(ProxySelector.class);

    private static final int AMAZING_SCORE = 100;

    @Resource
    private DomainIpRepository domainIpRepository;

    @Resource
    private ProxyRepository proxyRepository;

    /**
     * 域名与代理的映射
     */
    private Multimap<String, Proxy> domainProxyMap = HashMultimap.create();

    private List<Proxy> proxyList;

    public ProxySelector() {
    }

    public void init() {
        Iterable<Proxy> proxies = Collections2.filter(proxyRepository.findAvailable(),
                new Predicate<Proxy>() {
                    @Override
                    public boolean apply(Proxy proxy) {
                        return proxy.getAvailbelScore() > AMAZING_SCORE && proxy.getConnectionScore() > 100;
                    }
                });
        this.proxyList = Lists.newArrayList(proxies);
    }

    public Optional<Proxy> randomAvailableProxy(String domain) {
        Preconditions.checkNotNull(domain, "domain does not exists");
        List<Proxy> proxyCache = getProxies(domain);
        if (proxyCache.size() > 0) {
            log.info("proxyCache.size() is: {}", proxyCache.size());
            return proxyFromCache(proxyCache);
        }
        /*PageRequest pageRequest = new PageRequest(0, Integer.MAX_VALUE);
        List<DomainIp> domainIpList = domainIpRepository.selectAvailable(domain, pageRequest);
        if (domainIpList.size() > 0) {
            log.info("domainIpList.size() is: {}", domainIpList.size());
            return proxyFromDomainIp(domain, domainIpList);
        }*/
        return ProxyFromAv(domain);
    }

    private Optional<Proxy> ProxyFromAv(String domain) {
        int randomSize = (int) (Math.random() * proxyList.size());
        log.info("proxyList.size() is: {} randomSize is:{} ", proxyList.size(), randomSize);
        Proxy proxy = proxyList.get(randomSize);
        log.info("proxy  is: {}  ", proxy.getIp());
        //putDomain(domain, proxy);
        return Optional.of(proxy);
    }

    private Optional<Proxy> proxyFromDomainIp(String domain, List<DomainIp> domainIpList) {
        int randomSize = (int) (Math.random() * domainIpList.size());
        Long proxyId = domainIpList.get(randomSize).getProxyId();
        Proxy proxy = proxyRepository.selectByPrimaryKey(proxyId);
        putDomain(domain, proxy);
        return Optional.of(proxy);
    }

    private Optional<Proxy> proxyFromCache(List<Proxy> proxyCache) {
        int randomSize = (int) (Math.random() * proxyCache.size());
        return Optional.of(proxyCache.get(randomSize));
    }

    private List<Proxy> getProxies(String domain) {
        return Lists.newArrayList(domainProxyMap.get(domain));
    }

    private void putDomain(String domain, Proxy proxy) {
        domainProxyMap.put(domain, proxy);
    }
}
