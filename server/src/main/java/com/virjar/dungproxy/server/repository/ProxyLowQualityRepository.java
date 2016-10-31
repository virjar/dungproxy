package com.virjar.dungproxy.server.repository;

import com.virjar.dungproxy.server.entity.Proxy;
import org.springframework.stereotype.Repository;

/**
 * Created by nicholas on 9/19/2016.
 */
@Repository
public interface ProxyLowQualityRepository {
    int insert(Proxy proxy);

    int insertSelective(Proxy proxy);

    Proxy isExists(Long id);
}
