package com.virjar.distributer;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.entity.Proxy;
import com.virjar.model.ProxyModel;
import com.virjar.repository.DomainqueueRepository;
import com.virjar.repository.ProxyRepository;

/**
 * 分发IP资源 Created by virjar on 16/8/27.
 */
@Component

public class DistributeService {

    @Resource
    private ProxyRepository proxyRepository;
    @Resource
    private DomainqueueRepository domainqueueRepository;

    @Resource
    private BeanMapper beanMapper;

    private static final Logger logger = LoggerFactory.getLogger(DistributeService.class);

    public List<ProxyModel> distribute(RequestForm requestForm) {
        trimRequestForm(requestForm);
        Proxy proxy = genCondition(requestForm);
        List<Proxy> available = proxyRepository.find4Distribute(5000, proxy);
        available = filterUsed(requestForm.getUsedSign(), available);
        if (available.size() < requestForm.getNum() || "strict".equals(requestForm.getDistributeStrategy())) {
            return beanMapper.mapAsList(available, ProxyModel.class);
        }
        // TODO 如果数据多了,需要压缩排序?
        return beanMapper.mapAsList(available.subList(0, requestForm.getNum()), ProxyModel.class);
    }

    private List<Proxy> filterUsed(String usedSign, List<Proxy> dbProxy) {
        List<Proxy> ret = Lists.newArrayList();
        if (StringUtils.isEmpty(usedSign)) {
            ret.addAll(dbProxy);
            return ret;
        }
        DistributedSign sign = null;
        try {
            sign = DistributedSign.unSign(usedSign);
        } catch (Exception e) {
            logger.error("used unsigned failed ", e);
        }
        if (sign != null) {
            for (Proxy proxy : dbProxy) {
                if (!sign.contains(proxy)) {
                    ret.add(proxy);
                }
            }
        }
        return ret;
    }

    private Proxy genCondition(RequestForm requestForm) {
        Proxy queryProxy = new Proxy();
        if (requestForm.getMatchAddress()) {
            queryProxy.setCountry(requestForm.getCountry());
            queryProxy.setArea(requestForm.getArea());
        }
        if (requestForm.getMatchISP()) {
            queryProxy.setIsp(requestForm.getIsp());
        }
        // 翻墙还不支持
        if (requestForm.getUserInteral()) {
            queryProxy.setSupportGfw(true);
        }

        if (requestForm.getHeaderLoseAllow()) {
            queryProxy.setLostheader(false);
        }

        queryProxy.setSpeed((long) (int) requestForm.getMaxPing());
        return queryProxy;

    }

    private void trimRequestForm(RequestForm requestForm) {
        if (requestForm.getNum() == null || requestForm.getNum() < 0) {
            requestForm.setNum(10);
        }
        if (requestForm.getNum() > 200) {
            requestForm.setNum(200);
        }
        if (requestForm.getTransparent() == null || requestForm.getTransparent() > 2) {
            requestForm.setTransparent(-1);
        }
        if (BooleanUtils.isTrue(requestForm.getHeaderLoseAllow())) {
            requestForm.setHeaderLoseAllow(true);
        } else {
            requestForm.setHeaderLoseAllow(false);
        }
        if (!BooleanUtils.isTrue(requestForm.getMatchISP())) {
            requestForm.setMatchISP(false);
        }
        if (!BooleanUtils.isTrue(requestForm.getMatchAddress())) {
            requestForm.setMatchAddress(false);
        }
        if (!BooleanUtils.isTrue(requestForm.getUserInteral())) {
            requestForm.setUserInteral(false);
        }
        if (!BooleanUtils.isTrue(requestForm.getSupportHttps())) {
            requestForm.setSupportHttps(false);
        }
        if (requestForm.getMaxPing() == null || requestForm.getMaxPing() < 1) {
            requestForm.setMaxPing(Integer.MAX_VALUE);
        }
        if (!StringUtils.equals("strict", requestForm.getDistributeStrategy())
                && !StringUtils.equals("soft", requestForm.getDistributeStrategy())) {
            requestForm.setDistributeStrategy("soft");
        }
    }
}
