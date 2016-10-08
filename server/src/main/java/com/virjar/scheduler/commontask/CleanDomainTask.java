package com.virjar.scheduler.commontask;

import java.util.List;

import javax.annotation.Resource;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.virjar.model.DomainMetaModel;
import com.virjar.repository.DomainIpRepository;
import com.virjar.service.DomainMetaService;
import com.virjar.utils.SysConfig;

/**
 * 如果长时间没有请求对应domain,则删除对应数据,下线domainPool<br/>
 * Created by virjar on 16/10/8.
 */
@Component
public class CleanDomainTask extends CommonTask {

    private static final String DURATION = "common.task.duration.cleandomain";

    private static final String STEP = "common.task.step.cleandomain";
    private int beforeStep;
    private static final int batchSize = 100;
    @Resource
    private DomainMetaService domainMetaService;

    @Resource
    private DomainIpRepository domainIpRepository;

    public CleanDomainTask() {
        super(NumberUtils.toInt(SysConfig.getInstance().get(DURATION), 176400000));
        beforeStep = NumberUtils.toInt(SysConfig.getInstance().get(STEP), 10);
    }

    @Override
    public Object execute() {
        List<DomainMetaModel> domainMetaModels = domainMetaService.selectBefore(beforeStep,
                new PageRequest(0, batchSize));
        while (domainMetaModels.size() > 0) {
            for (DomainMetaModel domainMetaModel : domainMetaModels) {
                clean(domainMetaModel);
            }
            domainMetaModels = domainMetaService.selectBefore(beforeStep, new PageRequest(0, batchSize));
        }
        return null;
    }

    private void clean(DomainMetaModel domainMetaModel) {
        domainIpRepository.deleteByDomain(domainMetaModel.getDomain());
        domainMetaService.deleteByPrimaryKey(domainMetaModel.getId());
    }
}
