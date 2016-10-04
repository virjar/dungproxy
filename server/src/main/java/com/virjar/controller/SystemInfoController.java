package com.virjar.controller;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.virjar.core.rest.ResponseEnvelope;
import com.virjar.core.utils.ReturnUtil;
import com.virjar.crawler.Collector;
import com.virjar.scheduler.CollectorTask;
import com.virjar.scheduler.NonePortResourceTester;

/**
 * Created by virjar on 16/9/15.
 */
@Controller
@RequestMapping("/system")
public class SystemInfoController {
    private final Logger logger = LoggerFactory.getLogger(SystemInfoController.class);

    @RequestMapping(value = "/key", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ResponseEnvelope<Object>> testkey(HttpServletRequest request) {
        NonePortResourceTester.sendIp(request.getRemoteAddr());// 老系统,有其他人把请求打过来,我们记录数据,然后尝试入新库
        return ReturnUtil.retSuccess("if a client get this string,we think proxy is realy avaible. qq group 428174328");
    }

    @RequestMapping(value = "/static", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ResponseEnvelope<Object>> avaiable() {
        List<Collector> collecters = CollectorTask.getCollectors();
        if (collecters == null) {
            logger.info("server not start to collect proxy resource");
            return ReturnUtil.retSuccess(collecters);
        }
        List<Map<String, Object>> ret = Lists.newArrayList();
        for (Collector collecter : collecters) {
            Map<String, Object> item = Maps.newHashMap();
            item.put("batchSize", collecter.getBatchsize());
            item.put("lastUrl", collecter.getLastUrl());
            item.put("collectNumber", collecter.getGetnumber());
            item.put("hibtate", collecter.getHibrate());
            item.put("website", collecter.getWebsite());
            item.put("errorinfo", collecter.getErrorinfo());
            ret.add(item);
        }
        return ReturnUtil.retSuccess(ret);
    }
}
