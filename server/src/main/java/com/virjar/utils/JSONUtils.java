package com.virjar.utils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.util.TypeUtils;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

import java.util.List;

/**
 * 符合virjar框架规范的数据接口解析 Created by virjar on 16/8/27.
 */
public class JSONUtils {
    public static <T> T parse(String str, Class<T> clazz) {
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(str);
            Boolean status = jsonObject.getBoolean("status");
            if (BooleanUtils.isTrue(status)) {
                return jsonObject.getObject("data", clazz);
            }
        }catch (Exception e){
            //
        }
        return null;
    }

    public static <T> List<T> parseList(String str, Class<T> clazz) {
        if (StringUtils.isEmpty(str)) {
            return null;
        }
        try {
            JSONObject jsonObject = JSON.parseObject(str);
            Boolean status = jsonObject.getBoolean("status");
            if (BooleanUtils.isTrue(status)) {
                JSONArray jsonArray = jsonObject.getJSONArray("data");
                List<T> list = Lists.newArrayList();
                for (Object data: jsonArray.toArray()) {
                    list.add(TypeUtils.castToJavaBean(data, clazz));
                }
                return list;
            }
        }catch (Exception e){
            //
        }
        return null;
    }
}
