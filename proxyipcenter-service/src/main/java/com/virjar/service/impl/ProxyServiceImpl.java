package com.virjar.service.impl;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.entity.Proxy;
import com.virjar.model.ProxyModel;
import com.virjar.repository.ProxyRepository;
import com.virjar.service.ProxyService;
import com.virjar.utils.SysConfig;

@Service
public class ProxyServiceImpl implements ProxyService {
    @Resource
    private BeanMapper beanMapper;

    @Resource
    private ProxyRepository proxyRepo;

    private int validBatchSize;
    private int inValidBatchSize;

    @PostConstruct
    public void init() {
        int batchSize = SysConfig.getInstance().getValidateBatchSize();
        List<String> ratio = Splitter.on(":").splitToList(SysConfig.getInstance().getValidateBatchRatio());
        int validNum = NumberUtils.toInt(ratio.get(0));
        int inValidNum = NumberUtils.toInt(ratio.get(1));
        validBatchSize = batchSize * validNum / (validNum + inValidNum);
        inValidNum = batchSize - validBatchSize;
    }

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

    @Override
    public List<ProxyModel> find4availableupdate() {
        int slot, frame;
        Proxy queryProxy = new Proxy();
        List<Proxy> ret = Lists.newArrayList();
        int maxScore = proxyRepo.getMaxScore("availbelScore");
        int minScore = proxyRepo.getMinScore("availbelScore");
        int realValidBatchSize = validBatchSize;
        int realInvalidBatchSize = inValidBatchSize;
        if (maxScore == 0 && minScore == 0) {
            return beanMapper.mapAsList(
                    proxyRepo.selectPage(queryProxy,
                            new PageRequest(0, validBatchSize + inValidBatchSize,
                                    new Sort(new Sort.Order(Sort.Direction.DESC, "availbel_score_date")))),
                    ProxyModel.class);
        } else if (maxScore == 0) {
            realInvalidBatchSize = validBatchSize + inValidBatchSize;
        } else if (minScore == 0) {
            realValidBatchSize = validBatchSize + inValidBatchSize;
        }

        // 有效资源选取
        if (maxScore < SysConfig.getInstance().getSlotNumber()) {
            frame = 1;
            slot = maxScore;
        } else {
            frame = maxScore / SysConfig.getInstance().getSlotNumber();
            slot = SysConfig.getInstance().getSlotNumber();
        }

        int needsize = realValidBatchSize;
        for (int i = 0; i < slot - 1; i++) {// 模型，高级别槽获取数量是低级别的1/2， 大批量数据将会严格服从 log{SlotFactory}n。
            ret.addAll(proxyRepo.getfromSlot(i * frame, (i + 1) * frame,
                    needsize / SysConfig.getInstance().getSlotFactory(), "availbel_score_date", "availbel_score",
                    "connection_score > 0"));
            needsize = validBatchSize - ret.size();
        }
        ret.addAll(proxyRepo.getfromSlot((slot - 1) * frame, slot * frame + maxScore, needsize, "availbel_score_date",
                "availbel_score", "connection_score > 0"));

        // 无效资源选取
        if (minScore < -SysConfig.getInstance().getSlotNumber()) {
            frame = 1;
            slot = minScore;
        } else {
            frame = -minScore / SysConfig.getInstance().getSlotNumber();
            slot = SysConfig.getInstance().getSlotNumber();
        }

        needsize = realInvalidBatchSize;
        for (int i = 0; i < slot - 1; i++) {// 模型，高级别槽获取数量是低级别的1/2， 大批量数据将会严格服从 log{SlotFactory}n。
            ret.addAll(proxyRepo.getfromSlot((i + 1) * -frame, i * -frame,
                    needsize / SysConfig.getInstance().getSlotFactory(), "availbel_score_date", "availbel_score",
                    "connection_score > 0"));
            needsize = validBatchSize - ret.size();
        }
        ret.addAll(proxyRepo.getfromSlot(slot * -frame, (slot + 1) * -frame + maxScore, needsize, "availbel_score_date",
                "availbel_score", "connection_score > 0"));

        return beanMapper.mapAsList(ret, ProxyModel.class);
    }

    @Override
    public List<ProxyModel> find4connectionupdate() {
        int slot, frame;
        Proxy queryProxy = new Proxy();
        List<Proxy> ret = Lists.newArrayList();
        int maxScore = proxyRepo.getMaxScore("connection_score");
        int minScore = proxyRepo.getMinScore("connection_score");
        int realValidBatchSize = validBatchSize;
        int realInvalidBatchSize = inValidBatchSize;
        if (maxScore == 0 && minScore == 0) {
            return beanMapper.mapAsList(
                    proxyRepo.selectPage(queryProxy,
                            new PageRequest(0, validBatchSize + inValidBatchSize,
                                    new Sort(new Sort.Order(Sort.Direction.DESC, "connection_score_date")))),
                    ProxyModel.class);
        } else if (maxScore == 0) {
            realInvalidBatchSize = validBatchSize + inValidBatchSize;
        } else if (minScore == 0) {
            realValidBatchSize = validBatchSize + inValidBatchSize;
        }

        // 有效资源选取
        if (maxScore < SysConfig.getInstance().getSlotNumber()) {
            frame = 1;
            slot = maxScore;
        } else {
            frame = maxScore / SysConfig.getInstance().getSlotNumber();
            slot = SysConfig.getInstance().getSlotNumber();
        }

        int needsize = realValidBatchSize;
        for (int i = 0; i < slot - 1; i++) {// 模型，高级别槽获取数量是低级别的1/2， 大批量数据将会严格服从 log{SlotFactory}n。
            ret.addAll(proxyRepo.getfromSlot(i * frame, (i + 1) * frame,
                    needsize / SysConfig.getInstance().getSlotFactory(), "availbel_score_date", "connection_score",
                    null));
            needsize = validBatchSize - ret.size();
        }
        ret.addAll(proxyRepo.getfromSlot((slot - 1) * frame, slot * frame + maxScore, needsize, "connection_score_date",
                "connection_score", null));

        // 无效资源选取
        if (minScore < -SysConfig.getInstance().getSlotNumber()) {
            frame = 1;
            slot = minScore;
        } else {
            frame = -minScore / SysConfig.getInstance().getSlotNumber();
            slot = SysConfig.getInstance().getSlotNumber();
        }

        needsize = realInvalidBatchSize;
        for (int i = 0; i < slot - 1; i++) {// 模型，高级别槽获取数量是低级别的1/2， 大批量数据将会严格服从 log{SlotFactory}n。
            ret.addAll(proxyRepo.getfromSlot((i + 1) * -frame, i * -frame,
                    needsize / SysConfig.getInstance().getSlotFactory(), "connection_score_date", "connection_score",
                    null));
            needsize = validBatchSize - ret.size();
        }
        ret.addAll(proxyRepo.getfromSlot(slot * -frame, (slot + 1) * -frame + maxScore, needsize,
                "connection_score_date", "connection_score", null));

        return beanMapper.mapAsList(ret, ProxyModel.class);
    }
}