package com.virjar.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.virjar.model.DomainqueueModel;

public interface DomainqueueService {
    int create(DomainqueueModel domainqueueModel);

    int createSelective(DomainqueueModel domainqueueModel);

    DomainqueueModel findByPrimaryKey(Long id);

    int updateByPrimaryKey(DomainqueueModel domainqueueModel);

    int updateByPrimaryKeySelective(DomainqueueModel domainqueueModel);

    int deleteByPrimaryKey(Long id);

    int selectCount(DomainqueueModel domainqueueModel);

    List<DomainqueueModel> selectPage(DomainqueueModel domainqueueModel, Pageable Pageable);
}