package com.virjar.distributer;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.entity.Proxy;
import com.virjar.model.ProxyModel;
import com.virjar.repository.DomainqueueRepository;
import com.virjar.repository.ProxyRepository;

/**
 * 分发IP资源 Created by virjar on 16/8/27.
 */
@Component
public class DistributeService {

    @Resource
    private ProxyRepository proxyRepository;
    @Resource
    private DomainqueueRepository domainqueueRepository;

    @Resource
    private BeanMapper beanMapper;

    public List<ProxyModel> distribute(RequestForm requestForm) {
        java.util.List<Proxy> available = proxyRepository.findAvailable();
        return beanMapper.mapAsList(available, ProxyModel.class);
    }

}
