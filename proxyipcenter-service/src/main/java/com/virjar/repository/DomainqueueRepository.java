package com.virjar.repository;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.virjar.entity.Domainqueue;

@Repository
public interface DomainqueueRepository {
    int deleteByPrimaryKey(Long id);

    int insert(Domainqueue domainqueue);

    int insertSelective(Domainqueue domainqueue);

    Domainqueue selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Domainqueue domainqueue);

    int updateByPrimaryKey(Domainqueue domainqueue);

    int selectCount(Domainqueue domainqueue);

    List<Domainqueue> selectPage(Domainqueue domainqueue, Pageable pageable);
}