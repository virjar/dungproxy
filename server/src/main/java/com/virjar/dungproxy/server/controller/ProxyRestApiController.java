package com.virjar.dungproxy.server.controller;

import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
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

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.virjar.dungproxy.server.core.beanmapper.BeanMapper;
import com.virjar.dungproxy.server.core.rest.ResponseEnvelope;
import com.virjar.dungproxy.server.core.utils.ReturnCode;
import com.virjar.dungproxy.server.core.utils.ReturnUtil;
import com.virjar.dungproxy.server.distributer.DistributeService;
import com.virjar.dungproxy.server.distributer.DistributedSign;
import com.virjar.dungproxy.server.distributer.RequestForm;
import com.virjar.dungproxy.server.model.ProxyModel;
import com.virjar.dungproxy.server.service.ProxyService;
import com.virjar.dungproxy.server.vo.FeedBackForm;
import com.virjar.dungproxy.server.vo.ProxyVO;

@Controller
@RequestMapping("/proxyipcenter")
public class ProxyRestApiController {
    private final Logger logger = LoggerFactory.getLogger(ProxyRestApiController.class);

    @Resource
    private BeanMapper beanMapper;

    @Resource
    private ProxyService proxyService;

    @Resource
    private DistributeService distributeService;

    @RequestMapping(value = "/proxy/{id}", method = RequestMethod.GET)
    public ResponseEntity<ResponseEnvelope<Object>> getProxyById(@PathVariable Long id) {
        ProxyModel proxyModel = proxyService.findByPrimaryKey(id);
        ProxyVO proxyVO = beanMapper.map(proxyModel, ProxyVO.class);
        return ReturnUtil.retSuccess(proxyVO);
    }

    @RequestMapping(value = "/proxy", method = RequestMethod.POST)
    public ResponseEntity<ResponseEnvelope<Object>> createProxy(@RequestBody @Valid ProxyVO proxyVO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ReturnUtil.retException(ReturnCode.INPUT_PARAM_ERROR, errorMessage);
        }
        ProxyModel proxyModel = beanMapper.map(proxyVO, ProxyModel.class);
        Integer id = proxyService.create(proxyModel);
        return ReturnUtil.retSuccess(id);
    }

    @RequestMapping(value = "/proxy/{id}", method = RequestMethod.DELETE)
    public ResponseEntity<ResponseEnvelope<Object>> deleteProxyByPrimaryKey(@PathVariable Long id) {
        Integer result = proxyService.deleteByPrimaryKey(id);
        if (result == 1) {
            return ReturnUtil.retSuccess(result);
        } else {
            return ReturnUtil.retException(ReturnCode.RECORD_NOT_EXIST, "id=" + id);
        }
    }

    @RequestMapping(value = "/proxy/{id}", method = RequestMethod.PUT)
    public ResponseEntity<ResponseEnvelope<Object>> updateProxyByPrimaryKeySelective(@PathVariable Long id,
            @RequestBody @Valid ProxyVO proxyVO, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ReturnUtil.retException(ReturnCode.INPUT_PARAM_ERROR, errorMessage);
        }
        ProxyModel proxyModel = beanMapper.map(proxyVO, ProxyModel.class);
        proxyModel.setId(id);
        Integer result = proxyService.updateByPrimaryKeySelective(proxyModel);
        if (result == 1) {
            return ReturnUtil.retSuccess(id);
        } else {
            return ReturnUtil.retException(ReturnCode.RECORD_NOT_EXIST, "id=" + id);
        }
    }

    @RequestMapping(value = "/proxy/list")
    public ResponseEntity<ResponseEnvelope<Object>> listProxys(@PageableDefault Pageable pageable, ProxyVO proxyVO,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ReturnUtil.retException(ReturnCode.INPUT_PARAM_ERROR, errorMessage);
        }
        List<ProxyModel> proxyModels = proxyService.selectPage(beanMapper.map(proxyVO, ProxyModel.class), pageable);
        Page<ProxyVO> page = new PageImpl<ProxyVO>(beanMapper.mapAsList(proxyModels, ProxyVO.class), pageable,
                proxyService.selectCount(beanMapper.map(proxyVO, ProxyModel.class)));
        return ReturnUtil.retSuccess(page);
    }

    @RequestMapping("/av")
    public ResponseEntity<ResponseEnvelope<Object>> avaliable(RequestForm requestForm) {
        logger.info("distribute request:{}", JSONObject.toJSONString(requestForm));
        // clientID缓存
        if (StringUtils.isNotEmpty(requestForm.getClientID())) {
            String sign = distributeService.findSign(requestForm.getClientID());
            if (StringUtils.isNotEmpty(sign)) {
                logger.info("client:{} find a distribute sign:{}", requestForm.getClientID(), sign);
                requestForm.setUsedSign(sign);
            }
        }

        List<ProxyModel> distribute = distributeService.distribute(requestForm);
        Map<String, Object> ret = Maps.newHashMap();

        if (StringUtils.isNotEmpty(requestForm.getClientID())) {
            if (distribute.size() == 0) {
                distributeService.setSign(requestForm.getClientID(), DistributedSign.empty);
            } else {
                distributeService.resign(requestForm.getClientID(),
                        Lists.transform(distribute, new Function<ProxyModel, String>() {
                            @Override
                            public String apply(ProxyModel input) {
                                return input.getIp() + ":" + input.getPort();
                            }
                        }));
            }
        }

        if (distribute.size() == 0) {
            ret.put("sign", DistributedSign.empty);
        } else {
            ret.put("sign", DistributedSign.resign(requestForm.getUsedSign(), distribute));
        }
        ret.put("num", distribute.size());
        ret.put("data", beanMapper.mapAsList(distribute, ProxyVO.class));
        ResponseEntity<ResponseEnvelope<Object>> responseEnvelopeResponseEntity = ReturnUtil.retSuccess(ret);
        //取消日志
        //logger.info("distribute result:{}", JSONObject.toJSONString(responseEnvelopeResponseEntity));
        return responseEnvelopeResponseEntity;
    }

    /**
     * 只能是json
     * 
     * @param feedBackForm
     * @return
     */
    @RequestMapping("/feedBack")
    public ResponseEntity<ResponseEnvelope<Object>> feedBack(@RequestBody FeedBackForm feedBackForm,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            String errorMessage = bindingResult.getAllErrors().get(0).getDefaultMessage();
            return ReturnUtil.retException(ReturnCode.INPUT_PARAM_ERROR, errorMessage);
        }
        distributeService.feedBack(feedBackForm);
        return ReturnUtil.retSuccess("success");
    }

    @RequestMapping("/allAv")
    public ResponseEntity<ResponseEnvelope<Object>> allAvailable() {
        return ReturnUtil.retSuccess(proxyService.allAvailable());
    }
}