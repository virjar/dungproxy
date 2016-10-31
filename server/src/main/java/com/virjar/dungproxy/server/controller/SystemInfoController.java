package com.virjar.dungproxy.server.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.virjar.dungproxy.server.core.rest.ResponseEnvelope;
import com.virjar.dungproxy.server.core.utils.ReturnUtil;
import com.virjar.dungproxy.server.crawler.Collector;
import com.virjar.dungproxy.server.scheduler.CollectorTask;
import com.virjar.dungproxy.server.scheduler.NonePortResourceTester;

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
    public String avaiable() {
        JSONArray jsonArray = new JSONArray();
        List<Collector> collecters = CollectorTask.getCollectors();
        if (collecters == null) {
            logger.info("server not start to collect proxy resource");
            return jsonArray.toJSONString();
        }
        for (Collector collecter : collecters) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("batchSize", collecter.getBatchsize());
            jsonObject.put("lastUrl", collecter.getLastUrl());
            jsonObject.put("collectNumber", collecter.getGetnumber());
            jsonObject.put("hibtate", collecter.getHibrate());
            jsonObject.put("website", collecter.getWebsite());
            jsonObject.put("errorinfo", collecter.getErrorinfo());
            jsonArray.add(jsonObject);
        }
        return jsonArray.toJSONString();
    }
}
