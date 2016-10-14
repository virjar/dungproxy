package com.virjar.service;

import java.util.List;

import org.springframework.data.domain.Pageable;

import com.virjar.model.DomainMetaModel;

public interface DomainMetaService {
    int create(DomainMetaModel domainMetaModel);

    int createSelective(DomainMetaModel domainMetaModel);

    DomainMetaModel findByPrimaryKey(Long id);

    int updateByPrimaryKey(DomainMetaModel domainMetaModel);

    int updateByPrimaryKeySelective(DomainMetaModel domainMetaModel);

    int deleteByPrimaryKey(Long id);

    int selectCount(DomainMetaModel domainMetaModel);

    List<DomainMetaModel> selectPage(DomainMetaModel domainMetaModel, Pageable Pageable);

    List<DomainMetaModel> selectBefore(int days,Pageable pageable);
}