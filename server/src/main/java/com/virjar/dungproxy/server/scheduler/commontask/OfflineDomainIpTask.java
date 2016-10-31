package com.virjar.dungproxy.server.scheduler.commontask;

import javax.annotation.Resource;

import com.virjar.dungproxy.server.utils.SysConfig;
import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.virjar.dungproxy.server.service.DomainIpService;
import org.springframework.stereotype.Component;

/**
 * 下线域名IP池数据 Created by virjar on 16/10/3.
 */
@Component
public class OfflineDomainIpTask extends CommonTask {
    private static final Logger logger = LoggerFactory.getLogger(OfflineDomainIpTask.class);
    private static final String DURATION = "common.task.duration.offlineDomainIp";

    @Resource
    private DomainIpService domainIpService;

    public OfflineDomainIpTask() {
        super(NumberUtils.toInt(SysConfig.getInstance().get(DURATION), 10800000));
    }

    @Override
    public Object execute() {
        domainIpService.offline();
        return "";
    }
}
