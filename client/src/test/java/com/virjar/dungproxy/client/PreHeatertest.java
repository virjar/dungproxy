package com.virjar.dungproxy.client;

import com.alibaba.fastjson.JSONObject;
import com.virjar.dungproxy.client.ippool.PreHeater;

/**
 * Created by virjar on 16/11/28.
 */
public class PreHeatertest {
    public static void main(String[] args) {
        PreHeater preHeater = new PreHeater();
        preHeater.addTask("https://www.douban.com/group/explore").doPreHeat();
        System.out.println(JSONObject.toJSONString(preHeater.getStringDomainPoolMap()));
    }
}
