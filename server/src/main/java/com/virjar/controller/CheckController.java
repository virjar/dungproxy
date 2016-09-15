package com.virjar.controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.core.rest.ResponseEnvelope;
import com.virjar.core.utils.ReturnUtil;
import com.virjar.entity.Proxy;
import com.virjar.model.AvailbelCheckResponse;
import com.virjar.model.ProxyModel;
import com.virjar.service.ProxyService;
import com.virjar.utils.Constant;
import com.virjar.utils.ProxyUtil;
import com.virjar.utils.ResourceFilter;
import com.virjar.utils.Tranparent;

/**
 * Created by virjar on 16/8/14.
 */
@Controller
@RequestMapping("/proxyipcenter")
public class CheckController {

    @Resource
    private ProxyService proxyService;

    @Resource
    private BeanMapper beanMapper;

    @RequestMapping(value = "/checkIp", method = RequestMethod.GET, produces = MediaType.ALL_VALUE)
    public ResponseEntity<ResponseEnvelope<Object>> getDomainqueueById(HttpServletRequest request) {
        byte transparent = checkTransparent(request);
        String remoteAddr = request.getRemoteAddr();
        AvailbelCheckResponse availbelCheckResponse = new AvailbelCheckResponse();
        availbelCheckResponse.setTransparent(transparent);
        availbelCheckResponse.setKey(AvailbelCheckResponse.staticKey);
        availbelCheckResponse.setRemoteAddr(remoteAddr);
        String header = request.getHeader(Constant.HEADER_CHECK_HEADER);
        availbelCheckResponse.setLostHeader(StringUtils.isEmpty(header));

        String ip = request.getParameter("ip");
        String port = request.getParameter("port");
        if (!StringUtils.isEmpty(ip) && !StringUtils.isEmpty(port)) {
            Proxy proxy = new Proxy();
            proxy.setIp(ip);
            proxy.setPort(NumberUtils.toInt(port, 3128));
            proxy.setIpValue(ProxyUtil.toIPValue(ip));
            if (!ResourceFilter.contains(proxy)) {
                proxyService.create(beanMapper.map(port, ProxyModel.class));
            }
        }
        return ReturnUtil.retSuccess(availbelCheckResponse);
    }

    private byte checkTransparent(HttpServletRequest request) {

        String ipAddress = request.getHeader("x-forwarded-for");
        if (!StringUtils.isEmpty(ipAddress)) {
            if (StringUtils.equalsIgnoreCase("unknown", ipAddress)) {
                return Tranparent.anonymous.getValue();
            } else {
                return Tranparent.transparent.getValue();
            }
        }
        ipAddress = request.getHeader("Proxy-Client-IP");
        if (!StringUtils.isEmpty(ipAddress)) {
            if (StringUtils.equalsIgnoreCase("unknown", ipAddress)) {
                return Tranparent.anonymous.getValue();
            } else {
                return Tranparent.transparent.getValue();
            }
        }
        ipAddress = request.getHeader("http_vir");
        if (!StringUtils.isEmpty(ipAddress)) {
            if (StringUtils.equalsIgnoreCase("unknown", ipAddress)) {
                return Tranparent.anonymous.getValue();
            } else {
                return Tranparent.transparent.getValue();
            }
        }
        return Tranparent.highAnonymous.getValue();
    }
}
