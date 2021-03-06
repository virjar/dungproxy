package com.virjar.dungproxy.server.service.impl;

import java.util.Date;
import java.util.List;

import javax.annotation.Resource;

import org.joda.time.DateTime;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.virjar.dungproxy.server.core.beanmapper.BeanMapper;
import com.virjar.dungproxy.server.entity.DomainMeta;
import com.virjar.dungproxy.server.model.DomainMetaModel;
import com.virjar.dungproxy.server.repository.DomainMetaRepository;
import com.virjar.dungproxy.server.service.DomainMetaService;

@Service
public class DomainMetaServiceImpl implements DomainMetaService {
    @Resource
    private BeanMapper beanMapper;

    @Resource
    private DomainMetaRepository domainMetaRepo;

    @Transactional
    @Override
    public int create(DomainMetaModel domainMetaModel) {
        return domainMetaRepo.insert(beanMapper.map(domainMetaModel, DomainMeta.class));
    }

    @Transactional
    @Override
    public int createSelective(DomainMetaModel domainMetaModel) {
        return domainMetaRepo.insertSelective(beanMapper.map(domainMetaModel, DomainMeta.class));
    }

    @Transactional
    @Override
    public int deleteByPrimaryKey(Long id) {
        return domainMetaRepo.deleteByPrimaryKey(id);
    }

    @Transactional(readOnly = true)
    @Override
    public DomainMetaModel findByPrimaryKey(Long id) {
        DomainMeta domainMeta = domainMetaRepo.selectByPrimaryKey(id);
        return beanMapper.map(domainMeta, DomainMetaModel.class);
    }

    @Transactional(readOnly = true)
    @Override
    public int selectCount(DomainMetaModel domainMetaModel) {
        return domainMetaRepo.selectCount(beanMapper.map(domainMetaModel, DomainMeta.class));
    }

    @Transactional
    @Override
    public int updateByPrimaryKey(DomainMetaModel domainMetaModel) {
        return domainMetaRepo.updateByPrimaryKey(beanMapper.map(domainMetaModel, DomainMeta.class));
    }

    @Transactional
    @Override
    public int updateByPrimaryKeySelective(DomainMetaModel domainMetaModel) {
        return domainMetaRepo.updateByPrimaryKeySelective(beanMapper.map(domainMetaModel, DomainMeta.class));
    }

    @Transactional(readOnly = true)
    @Override
    public List<DomainMetaModel> selectPage(DomainMetaModel domainMetaModel, Pageable pageable) {
        if (domainMetaModel == null) {
            domainMetaModel = new DomainMetaModel();
        }
        if (pageable == null) {
            pageable = new PageRequest(0, Integer.MAX_VALUE);
        }
        List<DomainMeta> domainMetaList = domainMetaRepo.selectPage(beanMapper.map(domainMetaModel, DomainMeta.class),
                pageable);
        return beanMapper.mapAsList(domainMetaList, DomainMetaModel.class);
    }

    @Override
    public List<DomainMetaModel> selectBefore(int days, Pageable pageable) {
        Date beforeDate = DateTime.now().plusDays(days).toDate();
        List<DomainMeta> domainMetas = domainMetaRepo.selectBefore(beforeDate, pageable);
        return beanMapper.mapAsList(domainMetas, DomainMetaModel.class);
    }
}