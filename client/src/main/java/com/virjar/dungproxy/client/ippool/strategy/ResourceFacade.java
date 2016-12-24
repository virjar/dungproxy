package com.virjar.dungproxy.client.ippool.strategy;

import java.util.List;

import com.virjar.dungproxy.client.model.AvProxyVO;

/**
 * Created by virjar on 16/9/29.<br/>
 * 资源适配接口,负责导入数据以及反馈使用情况
 */
public interface ResourceFacade {

    List<AvProxyVO> importProxy(String domain, String testUrl, Integer number);

    void feedBack(String domain, List<AvProxyVO> avProxies, List<AvProxyVO> disableProxies);

    // for preHeater
    List<AvProxyVO> allAvailable();
}
