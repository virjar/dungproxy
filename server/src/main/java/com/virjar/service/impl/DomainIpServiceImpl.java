package com.virjar.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.entity.DomainIp;
import com.virjar.model.DomainIpModel;
import com.virjar.repository.DomainIpRepository;
import com.virjar.service.DomainIpService;

@Service
public class DomainIpServiceImpl implements DomainIpService {
    @Resource
    private BeanMapper beanMapper;

    @Resource
    private DomainIpRepository domainIpRepo;

    @Transactional
    @Override
    public int create(DomainIpModel domainIpModel) {
        return domainIpRepo.insert(beanMapper.map(domainIpModel, DomainIp.class));
    }

    @Transactional
    @Override
    public int createSelective(DomainIpModel domainIpModel) {
        return domainIpRepo.insertSelective(beanMapper.map(domainIpModel, DomainIp.class));
    }

    @Transactional
    @Override
    public int deleteByPrimaryKey(Long id) {
        return domainIpRepo.deleteByPrimaryKey(id);
    }

    @Transactional(readOnly = true)
    @Override
    public DomainIpModel findByPrimaryKey(Long id) {
        DomainIp domainIp = domainIpRepo.selectByPrimaryKey(id);
        return beanMapper.map(domainIp, DomainIpModel.class);
    }

    @Transactional(readOnly = true)
    @Override
    public int selectCount(DomainIpModel domainIpModel) {
        return domainIpRepo.selectCount(beanMapper.map(domainIpModel, DomainIp.class));
    }

    @Transactional
    @Override
    public int updateByPrimaryKey(DomainIpModel domainIpModel) {
        return domainIpRepo.updateByPrimaryKey(beanMapper.map(domainIpModel, DomainIp.class));
    }

    @Transactional
    @Override
    public int updateByPrimaryKeySelective(DomainIpModel domainIpModel) {
        return domainIpRepo.updateByPrimaryKeySelective(beanMapper.map(domainIpModel, DomainIp.class));
    }

    @Transactional(readOnly = true)
    @Override
    public List<DomainIpModel> selectPage(DomainIpModel domainIpModel, Pageable pageable) {
        List<DomainIp> domainIpList = domainIpRepo.selectPage(beanMapper.map(domainIpModel, DomainIp.class), pageable);
        return beanMapper.mapAsList(domainIpList, DomainIpModel.class);
    }
}