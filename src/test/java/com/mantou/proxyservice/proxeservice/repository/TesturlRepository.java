package com.mantou.proxyservice.proxeservice.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mantou.proxyservice.proxeservice.entity.Testurl;

@Repository
public interface TesturlRepository {
    int deleteByPrimaryKey(@Param("id") Integer id);

    int insert(@Param("testurl") Testurl testurl);

    int insertSelective(@Param("testurl") Testurl testurl);

    Testurl selectByPrimaryKey(@Param("id") Integer id);

    int updateByPrimaryKeySelective(@Param("testurl") Testurl testurl);

    int updateByPrimaryKey(@Param("testurl") Testurl testurl);

    int selectCount(@Param("testurl") Testurl testurl);

    List<Testurl> selectPage(@Param("testurl") Testurl testurl, @Param("pageable") Pageable pageable);
}