package com.virjar.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.virjar.model.DomainIpModel;
import com.virjar.model.ProxyModel;

public interface DomainIpService {
    int create(DomainIpModel domainIpModel);

    int createSelective(DomainIpModel domainIpModel);

    DomainIpModel findByPrimaryKey(Long id);

    int updateByPrimaryKey(DomainIpModel domainIpModel);

    int updateByPrimaryKeySelective(DomainIpModel domainIpModel);

    int deleteByPrimaryKey(Long id);

    int selectCount(DomainIpModel domainIpModel);

    List<DomainIpModel> selectPage(DomainIpModel domainIpModel, Pageable Pageable);

    List<ProxyModel> convert(List<DomainIpModel> domainIpModels);

    DomainIpModel get(String domain, String ip, Integer port);

    void offline();
}