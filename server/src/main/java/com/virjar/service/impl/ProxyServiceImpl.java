package com.virjar.service.impl;

import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.virjar.utils.ResourceFilter;
import com.virjar.utils.SysConfig;

@Service
public class ProxyServiceImpl implements ProxyService {
    @Resource
    private BeanMapper beanMapper;

    @Resource
    private ProxyRepository proxyRepo;

    private int avaliableValidBatchSize;
    private int avaliableInValidBatchSize;

    private int connectionValidBatchSize;
    private int connectionInValidBatchSize;

    private static final Logger logger = LoggerFactory.getLogger(ProxyServiceImpl.class);

    @PostConstruct
    public void init() {
        int avaliableValidateBatchSize = SysConfig.getInstance().getAvaliableValidateBatchSize();
        List<String> ratio = Splitter.on(":").splitToList(SysConfig.getInstance().getAvaliableValidateBatchRatio());
        int validNum = NumberUtils.toInt(ratio.get(0));
        int inValidNum = NumberUtils.toInt(ratio.get(1));
        avaliableValidBatchSize = avaliableValidateBatchSize * validNum / (validNum + inValidNum);
        avaliableInValidBatchSize = avaliableValidateBatchSize - avaliableValidBatchSize;

        int connectionValidateBatchSize = SysConfig.getInstance().getConnectionValidateBatchSize();
        ratio = Splitter.on(":").splitToList(SysConfig.getInstance().getConnectionValidateBatchRatio());
        validNum = NumberUtils.toInt(ratio.get(0));
        inValidNum = NumberUtils.toInt(ratio.get(1));
        connectionValidBatchSize = connectionValidateBatchSize * validNum / (validNum + inValidNum);
        connectionInValidBatchSize = connectionValidateBatchSize - connectionValidBatchSize;

        if (avaliableValidateBatchSize < SysConfig.getInstance().getAvaliableSlotFactory()
                || avaliableInValidBatchSize < SysConfig.getInstance().getAvaliableSlotFactory()
                || connectionInValidBatchSize < SysConfig.getInstance().getConnectionSlotFactory()
                || connectionValidBatchSize < SysConfig.getInstance().getConnectionSlotFactory()) {
            logger.error("error config:");
            throw new IllegalArgumentException("batch size and slot factory config error");
        }
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
        Integer maxScore = proxyRepo.getMaxScore("availbel_score");
        Integer minScore = proxyRepo.getMinScore("availbel_score");
        maxScore = maxScore == null ? 0 : maxScore;
        minScore = minScore == null ? 0 : minScore;
        int realValidBatchSize = avaliableValidBatchSize;
        int realInvalidBatchSize = avaliableInValidBatchSize;
        if (maxScore == 0 && minScore == 0) {
            return beanMapper.mapAsList(
                    proxyRepo.selectPage(queryProxy,
                            new PageRequest(0, avaliableValidBatchSize + avaliableInValidBatchSize,
                                    new Sort(new Sort.Order(Sort.Direction.DESC, "availbel_score_date")))),
                    ProxyModel.class);
        } else if (maxScore == 0) {
            realInvalidBatchSize = avaliableValidBatchSize + avaliableInValidBatchSize;
        } else if (minScore == 0) {
            realValidBatchSize = avaliableValidBatchSize + avaliableInValidBatchSize;
        }

        // 有效资源选取
        if (maxScore < SysConfig.getInstance().getAvaliableSlotNumber()) {
            frame = 1;
            slot = maxScore;
        } else {
            frame = maxScore / SysConfig.getInstance().getAvaliableSlotNumber();
            slot = SysConfig.getInstance().getAvaliableSlotNumber();
        }

        int needsize = realValidBatchSize;
        for (int i = 0; i < slot - 1; i++) {// 模型，高级别槽获取数量是低级别的1/2， 大批量数据将会严格服从 log{SlotFactory}n。
            ret.addAll(proxyRepo.getfromSlot(i * frame, (i + 1) * frame,
                    needsize / SysConfig.getInstance().getAvaliableSlotFactory(), "availbel_score_date",
                    "availbel_score", "connection_score > 0"));
            needsize = realValidBatchSize - ret.size();
        }
        ret.addAll(proxyRepo.getfromSlot((slot - 1) * frame, maxScore, needsize, "availbel_score_date",
                "availbel_score", "connection_score > 0"));
        // 无效资源选取
        if (minScore > (-SysConfig.getInstance().getAvaliableSlotNumber())) {
            frame = 1;
            slot = minScore;
        } else {
            frame = -minScore / SysConfig.getInstance().getAvaliableSlotNumber();
            slot = SysConfig.getInstance().getAvaliableSlotNumber();
        }

        needsize = realInvalidBatchSize + realValidBatchSize - ret.size();
        for (int i = 0; i < slot - 1; i++) {// 模型，高级别槽获取数量是低级别的1/2， 大批量数据将会严格服从 log{SlotFactory}n。
            ret.addAll(proxyRepo.getfromSlot((i + 1) * -frame, i * -frame,
                    needsize / SysConfig.getInstance().getAvaliableSlotFactory(), "availbel_score_date",
                    "availbel_score", "connection_score > 0"));

            needsize = realInvalidBatchSize + realValidBatchSize - ret.size();
        }
        ret.addAll(proxyRepo.getfromSlot(minScore, (slot - 1) * -frame, needsize, "availbel_score_date",
                "availbel_score", "connection_score > 0"));

        return beanMapper.mapAsList(ret, ProxyModel.class);
    }

    @Override
    public List<ProxyModel> find4connectionupdate() {
        int slot, frame;
        Proxy queryProxy = new Proxy();
        List<Proxy> ret = Lists.newArrayList();
        Integer maxScore = proxyRepo.getMaxScore("connection_score");
        Integer minScore = proxyRepo.getMinScore("connection_score");
        maxScore = maxScore == null ? 0 : maxScore;
        minScore = minScore == null ? 0 : minScore;
        int realValidBatchSize = connectionValidBatchSize;
        int realInvalidBatchSize = connectionInValidBatchSize;
        if (maxScore == 0 && minScore == 0) {
            return beanMapper.mapAsList(
                    proxyRepo.selectPage(queryProxy,
                            new PageRequest(0, connectionValidBatchSize + connectionInValidBatchSize,
                                    new Sort(new Sort.Order(Sort.Direction.DESC, "connection_score_date")))),
                    ProxyModel.class);
        } else if (maxScore == 0) {
            realInvalidBatchSize = connectionValidBatchSize + connectionInValidBatchSize;
        } else if (minScore == 0) {
            realValidBatchSize = connectionValidBatchSize + connectionInValidBatchSize;
        }

        // 有效资源选取
        if (maxScore < SysConfig.getInstance().getConnectionSlotNumber()) {
            frame = 1;
            slot = maxScore;
        } else {
            frame = maxScore / SysConfig.getInstance().getConnectionSlotNumber();
            slot = SysConfig.getInstance().getConnectionSlotNumber();
        }

        int needsize = realValidBatchSize;
        for (int i = 0; i < slot - 1; i++) {// 模型，高级别槽获取数量是低级别的1/2， 大批量数据将会严格服从 log{SlotFactory}n。
            ret.addAll(proxyRepo.getfromSlot(i * frame, (i + 1) * frame,
                    needsize / SysConfig.getInstance().getConnectionSlotFactory(), "availbel_score_date",
                    "connection_score", null));
            needsize = realValidBatchSize - ret.size();
        }
        ret.addAll(proxyRepo.getfromSlot((slot - 1) * frame, maxScore, needsize, "connection_score_date",
                "connection_score", null));

        // 无效资源选取
        if (minScore > (-SysConfig.getInstance().getConnectionSlotNumber())) {
            frame = 1;
            slot = minScore;
        } else {
            frame = -minScore / SysConfig.getInstance().getConnectionSlotNumber();
            slot = SysConfig.getInstance().getConnectionSlotNumber();
        }

        needsize = realInvalidBatchSize + realValidBatchSize - ret.size();
        for (int i = 0; i < slot - 1; i++) {// 模型，高级别槽获取数量是低级别的1/2， 大批量数据将会严格服从 log{SlotFactory}n。
            ret.addAll(proxyRepo.getfromSlot((i + 1) * -frame, i * -frame,
                    needsize / SysConfig.getInstance().getConnectionSlotFactory(), "connection_score_date",
                    "connection_score", null));
            needsize = realInvalidBatchSize + realValidBatchSize - ret.size();
        }
        ret.addAll(proxyRepo.getfromSlot(minScore, (slot - 1) * -frame, needsize, "connection_score_date",
                "connection_score", null));

        return beanMapper.mapAsList(ret, ProxyModel.class);
    }

    @Override
    public void save(List<ProxyModel> draftproxys) {
        for (Proxy proxy : beanMapper.mapAsList(draftproxys, Proxy.class)) {
            Proxy queryProxy = new Proxy();
            queryProxy.setIp(proxy.getIp());
            queryProxy.setPort(proxy.getPort());
            if (proxyRepo.selectCount(queryProxy) >= 1) {
                ResourceFilter.addconflict(proxy);
            } else {
                proxyRepo.insertSelective(proxy);
            }
        }
    }
}