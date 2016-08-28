package com.virjar.service.impl;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.entity.Domainqueue;
import com.virjar.model.DomainqueueModel;
import com.virjar.repository.DomainqueueRepository;
import com.virjar.service.DomainqueueService;

@Service
public class DomainqueueServiceImpl implements DomainqueueService {
    @Resource
    private BeanMapper beanMapper;

    @Resource
    private DomainqueueRepository domainqueueRepo;

    @Transactional
    @Override
    public int create(DomainqueueModel domainqueueModel) {
        return domainqueueRepo.insert(beanMapper.map(domainqueueModel, Domainqueue.class));
    }

    @Transactional
    @Override
    public int createSelective(DomainqueueModel domainqueueModel) {
        return domainqueueRepo.insertSelective(beanMapper.map(domainqueueModel, Domainqueue.class));
    }

    @Transactional
    @Override
    public int deleteByPrimaryKey(Long id) {
        return domainqueueRepo.deleteByPrimaryKey(id);
    }

    @Transactional(readOnly = true)
    @Override
    public DomainqueueModel findByPrimaryKey(Long id) {
        Domainqueue domainqueue = domainqueueRepo.selectByPrimaryKey(id);
        return beanMapper.map(domainqueue, DomainqueueModel.class);
    }

    @Transactional(readOnly = true)
    @Override
    public int selectCount(DomainqueueModel domainqueueModel) {
        return domainqueueRepo.selectCount(beanMapper.map(domainqueueModel, Domainqueue.class));
    }

    @Transactional
    @Override
    public int updateByPrimaryKey(DomainqueueModel domainqueueModel) {
        return domainqueueRepo.updateByPrimaryKey(beanMapper.map(domainqueueModel, Domainqueue.class));
    }

    @Transactional
    @Override
    public int updateByPrimaryKeySelective(DomainqueueModel domainqueueModel) {
        return domainqueueRepo.updateByPrimaryKeySelective(beanMapper.map(domainqueueModel, Domainqueue.class));
    }

    @Transactional(readOnly = true)
    @Override
    public List<DomainqueueModel> selectPage(DomainqueueModel domainqueueModel, Pageable pageable) {
        List<Domainqueue> domainqueueList = domainqueueRepo.selectPage(
                beanMapper.map(domainqueueModel, Domainqueue.class), pageable);
        return beanMapper.mapAsList(domainqueueList, DomainqueueModel.class);
    }
}