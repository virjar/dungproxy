package com.virjar.controller;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.virjar.core.rest.ResponseEnvelope;
import com.virjar.core.utils.ReturnUtil;
import com.virjar.model.AvailbelCheckResponse;
import com.virjar.utils.Tranparent;

/**
 * Created by virjar on 16/8/14.
 */
@Controller
@RequestMapping("/proxyipcenter")
public class CheckController {
    @RequestMapping(value = "/checkIp", method = RequestMethod.GET)
    public ResponseEntity<ResponseEnvelope<Object>> getDomainqueueById(HttpServletRequest request) {
        byte transparent = checkTransparent(request);
        String remoteAddr = request.getRemoteAddr();
        AvailbelCheckResponse availbelCheckResponse = new AvailbelCheckResponse();
        availbelCheckResponse.setTransparent(transparent);
        availbelCheckResponse.setKey(AvailbelCheckResponse.staticKey);
        availbelCheckResponse.setRemoteAddr(remoteAddr);
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
