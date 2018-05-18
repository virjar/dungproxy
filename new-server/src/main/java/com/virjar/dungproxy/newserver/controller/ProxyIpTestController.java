package com.virjar.dungproxy.newserver.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Maps;
import com.virjar.dungproxy.newserver.util.ReturnUtil;
import com.virjar.dungproxy.newserver.util.WebJsonResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by virjar on 2018/5/18.
 */
@RestController
@Slf4j
@RequestMapping("/check")
public class ProxyIpTestController {
    @RequestMapping(value = "/checkProxy", method = RequestMethod.GET)
    @ResponseBody
    public WebJsonResponse<?> checkIP(HttpServletRequest request) {
        //匿名性
        String transparent = checkTransparent(request);
        //被代理的ip
        String remoteAddress = request.getRemoteAddr();


        String ip = request.getParameter("ip");
        String port = request.getParameter("port");
        if (!StringUtils.isEmpty(ip) && !StringUtils.isEmpty(port)) {
           //TODO save the reported data
        }
        Map<String,String> data = Maps.newHashMap();
        data.put("transparent",transparent);
        data.put("remoteAddress",remoteAddress);
        return ReturnUtil.success(data);
    }

    private String checkTransparent(HttpServletRequest request) {

        String xForwardFor = request.getHeader("x-forwarded-for");
        if(StringUtils.equalsIgnoreCase(xForwardFor,"unknown")){
            //匿名代理,知道走了代理,但是无法解析代理地址
            return "anonymous";
        }
        if(StringUtils.isNotBlank(xForwardFor)){
            //知道走了代理,并且可以解析代理ip地址
            return "transparent";
        }

        String proxyClientIp = request.getHeader("Proxy-Client-IP");
        if(StringUtils.equalsIgnoreCase(proxyClientIp,"unknown")){
            return "anonymous";
        }
        if(StringUtils.isNotBlank(proxyClientIp)){
            return "transparent";
        }

        String ipAddress = request.getHeader("http_vir");
        if(StringUtils.equalsIgnoreCase(ipAddress,"unknown")){
            return "anonymous";
        }
        if(StringUtils.isNotBlank(ipAddress)){
            return "transparent";
        }
        //完全无法感知代理信息
        return "highAnonymous";
    }
}
