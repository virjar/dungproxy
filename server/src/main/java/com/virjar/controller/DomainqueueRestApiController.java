package com.virjar.controller;

import java.util.List;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.core.rest.ResponseEnvelope;
import com.virjar.core.utils.ReturnCode;
import com.virjar.core.utils.ReturnUtil;
import com.virjar.model.DomainqueueModel;
import com.virjar.service.DomainqueueService;
import com.virjar.vo.DomainqueueVO;

@Controller
@RequestMapping("/proxyipcenter")
public class DomainqueueRestApiController {
    private final Logger logger = LoggerFactory.getLogger(DomainqueueRestApiController.class);

    @Resource
    private BeanMapper beanMapper;

    @Resource
    private DomainqueueService domainqueueService;

    @RequestMapping(value = "/domainqueue/{id}", method = RequestMethod.GET)
    public ResponseEntity<ResponseEnvelope<Object>> getDomainqueueById(@PathVariable Long id) {
        DomainqueueModel domainqueueModel = domainqueueService.findByPrimaryKey(id);
        DomainqueueVO domainqueueVO = beanMapper.map(domainqueueModel, DomainqueueVO.class);
        return ReturnUtil.retSuccess(domainqueueVO);
    }

    @RequestMapping(value = "/domainqueue", method = RequestMethod.POST)
    public ResponseEntity<ResponseEnvelope<Object>> createDomainqueue(@RequestBody @Valid DomainqueueVO domainqueueVO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ReturnUtil.retException(ReturnCode.INPUT_PARAM_ERROR, errorMessage);
        }
        DomainqueueModel domainqueueModel = beanMapper.map(domainqueueVO, DomainqueueModel.class);
        Integer id = domainqueueService.create(domainqueueModel);
        return ReturnUtil.retSuccess(id);
    }

    @RequestMapping(value = "/domainqueue/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<ResponseEnvelope<Object>> deleteDomainqueueByPrimaryKey(@PathVariable Long id) {
        Integer result = domainqueueService.deleteByPrimaryKey(id);
        if (result == 1) {
            return ReturnUtil.retSuccess(result);
        } else {
            return ReturnUtil.retException(ReturnCode.RECORD_NOT_EXIST, "id=" + id);
        }
    }

    @RequestMapping(value = "/domainqueue/{id}", method = RequestMethod.PUT)
    public ResponseEntity<ResponseEnvelope<Object>> updateDomainqueueByPrimaryKeySelective(@PathVariable Long id,
            @RequestBody @Valid DomainqueueVO domainqueueVO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ReturnUtil.retException(ReturnCode.INPUT_PARAM_ERROR, errorMessage);
        }
        DomainqueueModel domainqueueModel = beanMapper.map(domainqueueVO, DomainqueueModel.class);
        domainqueueModel.setId(id);
        Integer result = domainqueueService.updateByPrimaryKeySelective(domainqueueModel);
        if (result == 1) {
            return ReturnUtil.retSuccess(id);
        } else {
            return ReturnUtil.retException(ReturnCode.RECORD_NOT_EXIST, "id=" + id);
        }
    }

    @RequestMapping(value = "/domainqueue/list")
    public ResponseEntity<ResponseEnvelope<Object>> listDomainqueues(@PageableDefault Pageable pageable,
            DomainqueueVO domainqueueVO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ReturnUtil.retException(ReturnCode.INPUT_PARAM_ERROR, errorMessage);
        }
        List<DomainqueueModel> domainqueueModels = domainqueueService.selectPage(
                beanMapper.map(domainqueueVO, DomainqueueModel.class), pageable);
        Page<DomainqueueVO> page = new PageImpl<DomainqueueVO>(beanMapper.mapAsList(domainqueueModels,
                DomainqueueVO.class), pageable, domainqueueService.selectCount(beanMapper.map(domainqueueVO,
                DomainqueueModel.class)));
        return ReturnUtil.retSuccess(page);
    }
}