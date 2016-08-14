package com.virjar.repository;

import java.util.List;

import org.apache.ibatis.annotations.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.virjar.entity.Proxy;

@Repository
public interface ProxyRepository {
    int deleteByPrimaryKey(Long id);

    int insert(Proxy proxy);

    int insertSelective(Proxy proxy);

    Proxy selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(Proxy proxy);

    int updateByPrimaryKey(Proxy proxy);

    int selectCount(Proxy proxy);

    List<Proxy> selectPage(Proxy proxy, Pageable pageable);

    List<Proxy> getfromSlot(@Param(value = "start") int start, @Param(value = "end") int end,
            @Param(value = "size") int size, @Param("timeColumnName") String timeColumnName,
            @Param("scoreColumnName") String scoreColumnName, @Param("condition") String condition);

    int getMaxScore(@Param("scoreName") String scoreName);

    int getMinScore(@Param("scoreName") String scoreName);
}