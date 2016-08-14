package com.mantou.proxyservice.proxeservice.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.mantou.proxyservice.proxeservice.entity.Proxy;

@Repository
public interface OldProxyRepository {
    int deleteByPrimaryKey(@Param("id") Integer id);

    int insert(@Param("proxy") Proxy proxy);

    int insertSelective(@Param("proxy") Proxy proxy);

    Proxy selectByPrimaryKey(@Param("id") Integer id);

    int updateByPrimaryKeySelective(@Param("proxy") Proxy proxy);

    int updateByPrimaryKey(@Param("proxy") Proxy proxy);

    int selectCount(@Param("proxy") Proxy proxy);

    List<Proxy> selectPage(@Param("proxy") Proxy proxy, @Param("pageable") Pageable pageable);

	int getMaxConnectionLevel();

	Collection<? extends Proxy> getfromConnectionslot(@Param(value = "slot") int slot, @Param(value = "frame") int frame, @Param(value = "size") int size);
	
	int getMaxAvailableLevel();

	Collection<? extends Proxy> getfromAvailableslot(@Param(value = "slot") int slot, @Param(value = "frame") int frame, @Param(value = "size") int size);

    List<Proxy> findAvailable();
}