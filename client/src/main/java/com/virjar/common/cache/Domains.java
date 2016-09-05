package com.virjar.common.cache;

/**
 * Description: 载入到内存中的Domains和Id的映射关系
 *
 * @author lingtong.fu
 * @version 2016-09-05 20:55
 */
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.virjar.model.DomainqueueModel;

import java.util.List;

public class Domains {
    /**
     * id与Domain映射, id <=> Domain
     */
    private static BiMap<Long, DomainqueueModel> idDomainMap = Maps.synchronizedBiMap(HashBiMap.<Long, DomainqueueModel>create());

    /**
     * 通过Id获取domain
     */
    public static DomainqueueModel getDomain(Long domainId) {
        return idDomainMap.get(domainId);
    }

    public static void putDomain(DomainqueueModel domain) {
        idDomainMap.put(domain.getId(), domain);
    }

    public static ImmutableSet<Long> getDomainIds() {
        synchronized (idDomainMap) {
            return ImmutableSet.copyOf(idDomainMap.keySet());
        }
    }

    public static void setDomains(List<DomainqueueModel> domainList) {
        BiMap<Long, DomainqueueModel> tmpBiMap = Maps.synchronizedBiMap(HashBiMap.<Long, DomainqueueModel>create());
        for (DomainqueueModel domain : domainList) {
            tmpBiMap.put(domain.getId(), domain);
        }

        synchronized (Domains.class) {
            idDomainMap = tmpBiMap;
        }
    }

    private static ThreadLocal<DomainqueueModel> domainParam = new ThreadLocal<DomainqueueModel>() {

        @Override
        protected DomainqueueModel initialValue() {
            return new DomainqueueModel();
        }

        @Override
        public DomainqueueModel get() {
            DomainqueueModel d = super.get();
            return d;
        }
    };

    /**
     * 通过domain获取id
     */
    public static Long getDomainId() {
        DomainqueueModel domain = domainParam.get();
        return idDomainMap.inverse().get(domain);
    }

}