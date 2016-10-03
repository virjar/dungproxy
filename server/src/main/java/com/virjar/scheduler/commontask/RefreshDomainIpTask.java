package com.virjar.scheduler.commontask;

import java.util.List;
import java.util.Random;

import javax.annotation.Resource;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.virjar.model.DomainIpModel;
import com.virjar.model.DomainMetaModel;
import com.virjar.scheduler.DomainTestTask;
import com.virjar.service.DomainIpService;
import com.virjar.service.DomainMetaService;
import com.virjar.utils.SysConfig;

/**
 * Created by virjar on 16/10/3.
 */
@Component
public class RefreshDomainIpTask extends CommonTask {
    private static final Logger logger = LoggerFactory.getLogger(RefreshDomainIpTask.class);
    private static final String DURATION = "common.task.duration.freshDomainIp";

    @Resource
    private DomainMetaService domainMetaService;
    @Resource
    private DomainIpService domainIpService;
    private Random random = new Random(System.currentTimeMillis());

    public RefreshDomainIpTask() {
        super(NumberUtils.toInt(SysConfig.getInstance().get(DURATION), 176400000));
    }

    @Override
    public Object execute() {
        List<DomainMetaModel> domainMetaModels = domainMetaService.selectPage(null, null);
        for (DomainMetaModel domainMetaModel : domainMetaModels) {
            DomainIpModel query = new DomainIpModel();
            query.setDomain(domainMetaModel.getDomain());
            List<DomainIpModel> domainIpModels = domainIpService.selectPage(query, new PageRequest(0, 5));
            if (domainMetaModels.size() < 1) {
                continue;
            }
            DomainTestTask.sendDomainTask(domainIpModels.get(random.nextInt(domainIpModels.size())).getTestUrl());
        }
        return null;
    }
}
