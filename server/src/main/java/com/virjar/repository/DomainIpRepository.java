package com.virjar.repository;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.virjar.entity.DomainIp;

@Repository
public interface DomainIpRepository {
    int deleteByPrimaryKey(Long id);

    int insert(DomainIp domainip);

    int insertSelective(DomainIp domainip);

    DomainIp selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(DomainIp domainip);

    int updateByPrimaryKey(DomainIp domainip);

    int selectCount(DomainIp domainip);

    List<DomainIp> selectPage(DomainIp domainip, Pageable pageable);

    List<DomainIp> selectAvailable(@Param("domain") String domain, @Param("pageable") Pageable pageable);

    List<DomainIp> selectDisable(@Param("pageable") Pageable pageable);

    int deleteBatch(@Param("ids") List<Long> ids);

    int deleteByDomain(@Param("domian") String domain);
}