package com.virjar.dungproxy.client.ippool.strategy.resource;

import java.util.List;

import com.virjar.dungproxy.client.model.AvProxy;

/**
 * Created by virjar on 16/9/29.<br/>
 * 资源适配接口,负责导入数据以及反馈使用情况
 */
public interface ResourceFacade {

    List<AvProxy> importProxy(String domain, String testUrl, Integer number);

    void feedBack(String domain, List<AvProxy> avProxies, List<AvProxy> disableProxies);

    //for preHeater
    List<AvProxy> allAvailable();
}
