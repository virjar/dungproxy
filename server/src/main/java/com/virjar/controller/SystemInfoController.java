package com.virjar.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.virjar.core.rest.ResponseEnvelope;
import com.virjar.core.utils.ReturnUtil;

/**
 * Created by virjar on 16/9/15.
 */
@Controller
@RequestMapping("/system")
public class SystemInfoController {
    private final Logger logger = LoggerFactory.getLogger(SystemInfoController.class);

    @RequestMapping(value = "/key", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<ResponseEnvelope<Object>> testkey() {

        return ReturnUtil.retSuccess("if a client get this string,we think proxy is realy avaible. qq group 428174328");
    }
}
