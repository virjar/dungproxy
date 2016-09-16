package com.virjar.service;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Pageable;

import com.virjar.model.DomainIpModel;

public interface DomainIpService {
    int create(DomainIpModel domainIpModel);

    int createSelective(DomainIpModel domainIpModel);

    DomainIpModel findByPrimaryKey(Long id);

    int updateByPrimaryKey(DomainIpModel domainIpModel);

    int updateByPrimaryKeySelective(DomainIpModel domainIpModel);

    int deleteByPrimaryKey(Long id);

    int selectCount(DomainIpModel domainIpModel);

    List<DomainIpModel> selectPage(DomainIpModel domainIpModel, Pageable Pageable);
}