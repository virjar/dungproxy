package com.virjar.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.entity.Proxy;
import com.virjar.model.ProxyModel;
import com.virjar.repository.ProxyRepository;
import com.virjar.service.ProxyService;

@Service
public class ProxyServiceImpl implements ProxyService {
    @Resource
    private BeanMapper beanMapper;

    @Resource
    private ProxyRepository proxyRepo;

    @Transactional
    @Override
    public int create(ProxyModel proxyModel) {
        return proxyRepo.insert(beanMapper.map(proxyModel, Proxy.class));
    }

    @Transactional
    @Override
    public int createSelective(ProxyModel proxyModel) {
        return proxyRepo.insertSelective(beanMapper.map(proxyModel, Proxy.class));
    }

    @Transactional
    @Override
    public int deleteByPrimaryKey(Long id) {
        return proxyRepo.deleteByPrimaryKey(id);
    }

    @Transactional(readOnly = true)
    @Override
    public ProxyModel findByPrimaryKey(Long id) {
        Proxy proxy = proxyRepo.selectByPrimaryKey(id);
        return beanMapper.map(proxy, ProxyModel.class);
    }

    @Transactional(readOnly = true)
    @Override
    public int selectCount(ProxyModel proxyModel) {
        return proxyRepo.selectCount(beanMapper.map(proxyModel, Proxy.class));
    }

    @Transactional
    @Override
    public int updateByPrimaryKey(ProxyModel proxyModel) {
        return proxyRepo.updateByPrimaryKey(beanMapper.map(proxyModel, Proxy.class));
    }

    @Transactional
    @Override
    public int updateByPrimaryKeySelective(ProxyModel proxyModel) {
        return proxyRepo.updateByPrimaryKeySelective(beanMapper.map(proxyModel, Proxy.class));
    }

    @Transactional(readOnly = true)
    @Override
    public List<ProxyModel> selectPage(ProxyModel proxyModel, Pageable pageable) {
        List<Proxy> proxyList = proxyRepo.selectPage(beanMapper.map(proxyModel, Proxy.class), pageable);
        return beanMapper.mapAsList(proxyList, ProxyModel.class);
    }
}