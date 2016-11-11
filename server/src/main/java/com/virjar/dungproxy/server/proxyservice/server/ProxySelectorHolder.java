package com.virjar.dungproxy.server.proxyservice.server;

import com.google.common.base.Preconditions;
import com.virjar.dungproxy.server.entity.DomainIp;
import com.virjar.dungproxy.server.entity.Proxy;
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
public class ProxySelectorHolder {

    private static final Logger log = LoggerFactory.getLogger(ProxySelectorHolder.class);

    @Resource
    private DomainIpRepository domainIpRepository;

    @Resource
    private ProxyRepository proxyRepository;

    public Proxy selectProxySelector(String domain) {
        Preconditions.checkNotNull(domain, "domain does not exists");
        // 获取Proxy
        PageRequest pageRequest = new PageRequest(0, Integer.MAX_VALUE);
        List<DomainIp> domainIpList = domainIpRepository.selectAvailable(domain, pageRequest);
        Long proxyId = domainIpList.get(0).getProxyId();
        return proxyRepository.selectByPrimaryKey(proxyId);
    }
}
