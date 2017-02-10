package com.virjar.dungproxy.client.ippool.strategy.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Preconditions;
import com.virjar.dungproxy.client.ippool.strategy.AvProxyDumper;
import com.virjar.dungproxy.client.model.AvProxyVO;

/**
 * Created by virjar on 17/2/10.
 */
public class AvProxyDumperWrapper implements AvProxyDumper {
    private AvProxyDumper delegate;
    private AtomicBoolean isDumpring = new AtomicBoolean(false);

    public AvProxyDumperWrapper(AvProxyDumper delegate) {
        Preconditions.checkNotNull(delegate);
        this.delegate = delegate;
    }

    @Override
    public void setDumpFileName(String dumpFileName) {
        delegate.setDumpFileName(dumpFileName);
    }

    /**
     * 做一个包装,在数据为空的时候,放弃dump,避免并发dump
     * 
     * @param data 可用IP数据
     */
    @Override
    public void serializeProxy(Map<String, List<AvProxyVO>> data) {
        if (data == null || data.size() == 0) {
            return;
        }
        if (isDumpring.compareAndSet(false, true)) {
            try {
                delegate.serializeProxy(data);
            } finally {
                isDumpring.set(false);
            }
        }
    }

    @Override
    public Map<String, List<AvProxyVO>> unSerializeProxy() {
        return delegate.unSerializeProxy();
    }
}
