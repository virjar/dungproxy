package com.virjar.ippool.schedule;

import com.google.common.collect.Lists;
import com.virjar.model.AvProxy;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Description: AvProxyCollector
 *
 * @author lingtong.fu
 * @version 2016-09-11 17:45
 */
public class AvProxyCollector {

    private List<AvProxy> avProxies = Lists.newArrayList();
    private Integer pollingIndex;
    private ConcurrentLinkedQueue<AvProxy> avProxiesQueue;

    public AvProxy pollingProxy(){
        return null;
    }
    // 更新
    public void updateAvProxy() {

    }
}
