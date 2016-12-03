/*
 * Copyright 1999-2101 Alibaba Group Holding Ltd. Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package com.virjar.dungproxy.client.ippool.support.http.stat;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.virjar.dungproxy.client.ippool.support.http.PoolManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;

/**
 * 注意：避免直接调用Druid相关对象例如DruidDataSource等，相关调用要到DruidStatManagerFacade里用反射实现
 * 
 * @author sandzhang[sandzhangtoo@gmail.com]
 */
public final class DrungClientStatService implements DruidStatServiceMBean {

    private final static Logger LOG = LoggerFactory.getLogger(DrungClientStatService.class);

    public final static String MBEAN_NAME = "com.alibaba.druid:type=DrungClientStatService";

    private final static DrungClientStatService instance = new DrungClientStatService();

    public final static int RESULT_CODE_SUCCESS = 1;
    public final static int RESULT_CODE_ERROR = -1;

    private final static int DEFAULT_PAGE = 1;
    private final static int DEFAULT_PER_PAGE_COUNT = Integer.MAX_VALUE;
    private static final String ORDER_TYPE_DESC = "desc";
    private static final String ORDER_TYPE_ASC = "asc";
    private static final String DEFAULT_ORDER_TYPE = ORDER_TYPE_ASC;
    private static final String DEFAULT_ORDERBY = "SQL";

    private PoolManager poolManager = PoolManager.instance;

    private DrungClientStatService() {
    }

    public static DrungClientStatService getInstance() {
        return instance;
    }

    public String service(String url) {

        Map<String, String> parameters = getParameters(url);
        if (url.equals("/basic.json")) {
            return returnJSONResult(RESULT_CODE_SUCCESS, poolManager.returnJSONBasicStat());
        }
        if(url.equals("/domains.json")){
            return returnJSONResult(RESULT_CODE_SUCCESS, poolManager.domainInfo());
        }
        return returnJSONResult(RESULT_CODE_ERROR, "Do not support this request, please contact with administrator.");
    }

    public static String returnJSONResult(int resultCode, Object content) {
        Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
        dataMap.put("ResultCode", resultCode);
        dataMap.put("Content", content);
        return JSONObject.toJSONString(dataMap);
    }

    public static void registerMBean() {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        try {

            ObjectName objectName = new ObjectName(MBEAN_NAME);
            if (!mbeanServer.isRegistered(objectName)) {
                mbeanServer.registerMBean(instance, objectName);
            }
        } catch (JMException ex) {
            LOG.error("register mbean error", ex);
        }
    }

    public static void unregisterMBean() {
        MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();

        try {
            mbeanServer.unregisterMBean(new ObjectName(MBEAN_NAME));
        } catch (JMException ex) {
            LOG.error("unregister mbean error", ex);
        }
    }

    public static Map<String, String> getParameters(String url) {
        if (url == null || (url = url.trim()).length() == 0) {
            return Collections.<String, String> emptyMap();
        }

        String parametersStr = StringUtils.substring(url, url.indexOf('?'));
        if (parametersStr == null || parametersStr.length() == 0) {
            return Collections.<String, String> emptyMap();
        }

        String[] parametersArray = parametersStr.split("&");
        Map<String, String> parameters = new LinkedHashMap<String, String>();

        for (String parameterStr : parametersArray) {
            int index = parameterStr.indexOf("=");
            if (index <= 0) {
                continue;
            }

            String name = parameterStr.substring(0, index);
            String value = parameterStr.substring(index + 1);
            parameters.put(name, value);
        }
        return parameters;
    }

}
