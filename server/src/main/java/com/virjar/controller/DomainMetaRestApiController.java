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
import com.virjar.model.DomainMetaModel;
import com.virjar.service.DomainMetaService;
import com.virjar.vo.DomainMetaVO;

@Controller
@RequestMapping("/proxyip2")
public class DomainMetaRestApiController {
    private final Logger logger = LoggerFactory.getLogger(DomainMetaRestApiController.class);

    @Resource
    private BeanMapper beanMapper;

    @Resource
    private DomainMetaService domainMetaService;

    @RequestMapping(value = "/domainmeta/{id}", method = RequestMethod.GET)
    public ResponseEntity<ResponseEnvelope<Object>> getDomainMetaById(@PathVariable Long id) {
        DomainMetaModel domainMetaModel = domainMetaService.findByPrimaryKey(id);
        DomainMetaVO domainMetaVO = beanMapper.map(domainMetaModel, DomainMetaVO.class);
        return ReturnUtil.retSuccess(domainMetaVO);
    }

    @RequestMapping(value = "/domainmeta", method = RequestMethod.POST)
    public ResponseEntity<ResponseEnvelope<Object>> createDomainMeta(@RequestBody @Valid DomainMetaVO domainMetaVO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ReturnUtil.retException(ReturnCode.INPUT_PARAM_ERROR, errorMessage);
        }
        DomainMetaModel domainMetaModel = beanMapper.map(domainMetaVO, DomainMetaModel.class);
        Integer id = domainMetaService.create(domainMetaModel);
        return ReturnUtil.retSuccess(id);
    }

    @RequestMapping(value = "/domainmeta/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<ResponseEnvelope<Object>> deleteDomainMetaByPrimaryKey(@PathVariable Long id) {
        Integer result = domainMetaService.deleteByPrimaryKey(id);
        if (result == 1) {
            return ReturnUtil.retSuccess(result);
        } else {
            return ReturnUtil.retException(ReturnCode.RECORD_NOT_EXIST, "id=" + id);
        }
    }

    @RequestMapping(value = "/domainmeta/{id}", method = RequestMethod.PUT)
    public ResponseEntity<ResponseEnvelope<Object>> updateDomainMetaByPrimaryKeySelective(@PathVariable Long id,
            @RequestBody @Valid DomainMetaVO domainMetaVO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ReturnUtil.retException(ReturnCode.INPUT_PARAM_ERROR, errorMessage);
        }
        DomainMetaModel domainMetaModel = beanMapper.map(domainMetaVO, DomainMetaModel.class);
        domainMetaModel.setId(id);
        Integer result = domainMetaService.updateByPrimaryKeySelective(domainMetaModel);
        if (result == 1) {
            return ReturnUtil.retSuccess(id);
        } else {
            return ReturnUtil.retException(ReturnCode.RECORD_NOT_EXIST, "id=" + id);
        }
    }

    @RequestMapping(value = "/domainmeta/list")
    public ResponseEntity<ResponseEnvelope<Object>> listDomainMetas(@PageableDefault Pageable pageable,
            DomainMetaVO domainMetaVO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ReturnUtil.retException(ReturnCode.INPUT_PARAM_ERROR, errorMessage);
        }
        List<DomainMetaModel> domainMetaModels = domainMetaService.selectPage(
                beanMapper.map(domainMetaVO, DomainMetaModel.class), pageable);
        Page<DomainMetaVO> page = new PageImpl<DomainMetaVO>(
                beanMapper.mapAsList(domainMetaModels, DomainMetaVO.class), pageable,
                domainMetaService.selectCount(beanMapper.map(domainMetaVO, DomainMetaModel.class)));
        return ReturnUtil.retSuccess(page);
    }
}