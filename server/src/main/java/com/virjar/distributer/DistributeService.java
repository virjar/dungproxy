package com.virjar.distributer;

import javax.annotation.Resource;

import org.springframework.stereotype.Component;

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

}
