package com.virjar.dungproxy.server.distributer;

import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.virjar.dungproxy.client.model.AvProxy;
import com.virjar.dungproxy.client.util.CommonUtil;
import com.virjar.dungproxy.server.core.beanmapper.BeanMapper;
import com.virjar.dungproxy.server.entity.DomainIp;
import com.virjar.dungproxy.server.entity.Proxy;
import com.virjar.dungproxy.server.model.DomainIpModel;
import com.virjar.dungproxy.server.model.ProxyModel;
import com.virjar.dungproxy.server.repository.DomainIpRepository;
import com.virjar.dungproxy.server.repository.ProxyRepository;
import com.virjar.dungproxy.server.scheduler.DomainTestTask;
import com.virjar.dungproxy.server.service.DomainIpService;
import com.virjar.dungproxy.server.service.ProxyService;
import com.virjar.dungproxy.server.vo.FeedBackForm;

/**
 * 分发IP资源 Created by virjar on 16/8/27.
 */
@Component

public class DistributeService {

    @Resource
    private ProxyRepository proxyRepository;

    @Resource
    private DomainIpRepository domainIpRepository;

    @Resource
    private DomainIpService domainIpService;

    @Resource
    private ProxyService proxyService;

    @Resource
    private BeanMapper beanMapper;

    private static final Logger logger = LoggerFactory.getLogger(DistributeService.class);

    public Boolean feedBack(FeedBackForm feedBackForm) {
        List<AvProxy> avProxys = feedBackForm.getAvProxy();
        for (AvProxy avProxy : avProxys) {
            if (avProxy.getReferCount() == 0) {
                continue;// 这个时候不计入可用IP
            }
            DomainIpModel domainIpModel = domainIpService.get(feedBackForm.getDomain(), avProxy.getIp(),
                    avProxy.getPort());
            if (domainIpModel == null) {
                continue;
                /*
                 * domainIpModel = new DomainIpModel(); domainIpModel.setCreatetime(new Date());
                 * domainIpModel.setIp(avProxy.getIp()); domainIpModel.setPort(avProxy.getPort());
                 * domainIpModel.setProxyId(0L);//TODO domainIpModel.setTestUrl();
                 */
            }
            domainIpModel.setDomainScore(domainIpModel.getDomainScore() + 1);
            domainIpModel.setDomainScoreDate(new Date());
            domainIpService.updateByPrimaryKeySelective(domainIpModel);
        }
        for (AvProxy avProxy : feedBackForm.getDisableProxy()) {
            DomainIpModel domainIpModel = domainIpService.get(feedBackForm.getDomain(), avProxy.getIp(),
                    avProxy.getPort());
            if (domainIpModel == null) {
                continue;
            }
            if (domainIpModel.getDomainScore() > -1) {//
                domainIpModel.setDomainScore(-1L);
            } else {
                domainIpModel.setDomainScore(domainIpModel.getDomainScore() - 1);
            }
            domainIpModel.setDomainScoreDate(new Date());
            domainIpService.updateByPrimaryKeySelective(domainIpModel);
        }
        return true;
    }

    public List<ProxyModel> distribute(RequestForm requestForm) {
        trimRequestForm(requestForm);

        List<ProxyModel> ret;
        // 第一步,查询domain库
        List<DomainIp> domainTestedProxys = get4DomainTested(requestForm);
        ret = domainIpService.convert(beanMapper.mapAsList(domainTestedProxys, DomainIpModel.class));
        ret = filterUsed(requestForm.getUsedSign(), ret);
        if (ret.size() > requestForm.getNum()) {
            return ret.subList(0, requestForm.getNum());
        }

        // TODO 第二步,由调度任务跑出domain的元数据,根据元数据查询
        // 元数据模块还没有实现好,暂时不实现这个模块

        // 第三步,根据查询参数查询
        Proxy proxy = genCondition(requestForm);// 现在这里比较慢,等待韦轩分表优化后观察
        List<ProxyModel> available = beanMapper.mapAsList(proxyRepository.find4Distribute(1500 - ret.size(), proxy),
                ProxyModel.class);
        available = filterUsed(requestForm.getUsedSign(), available);
        ret = merge(ret, available);
        if (ret.size() < requestForm.getNum() || "strict".equals(requestForm.getDistributeStrategy())) {
            return ret;
        }

        if (ret.size() >= requestForm.getNum()) {
            return ret.subList(0, requestForm.getNum());
        }

        // 第四步,随即选取资源填充
        // TODO
        return ret;
    }

    private List<ProxyModel> merge(List<ProxyModel> list1, List<ProxyModel> list2) {
        Set<ProxyModel> set = Sets.newHashSet(list1);
        set.addAll(list2);
        return Lists.newArrayList(set);
    }

    private List<DomainIp> get4DomainTested(RequestForm requestForm) {
        String checkUrl = requestForm.getCheckUrl();

        String domain = CommonUtil.extractDomain(checkUrl);
        if (StringUtils.isEmpty(domain)) {
            domain = requestForm.getDomain();
        }
        if (StringUtils.isEmpty(domain)) {
            return Lists.newArrayList();
        }
        List<DomainIp> domainIps = domainIpRepository.selectAvailable(domain, new PageRequest(0, Integer.MAX_VALUE));
        if (StringUtils.isNotEmpty(checkUrl)) {// 当前url正在使用,属于活跃域名,所以发送到检查模块,没有毛病
            DomainTestTask.sendDomainTask(checkUrl);
        }
        return domainIps;
    }

    private List<ProxyModel> filterUsed(String usedSign, List<ProxyModel> dbProxy) {
        List<ProxyModel> ret = Lists.newArrayList();
        if (StringUtils.isEmpty(usedSign)) {
            ret.addAll(dbProxy);
            return ret;
        }
        try {
            DistributedSign sign = DistributedSign.unSign(usedSign);
            if (sign != null) {
                for (ProxyModel proxy : dbProxy) {
                    if (!sign.contains(proxy)) {
                        ret.add(proxy);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("过滤已使用资源失败 ", e);
            return  dbProxy;
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

        if (requestForm.getTransparent() != null) {
            queryProxy.setTransperent((byte) (0xff & requestForm.getTransparent()));
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
