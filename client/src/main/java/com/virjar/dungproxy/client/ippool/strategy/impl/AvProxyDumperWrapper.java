package com.virjar.dungproxy.client.ippool.strategy.impl;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
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
                delegate.serializeProxy(Maps.transformValues(data, new Function<List<AvProxyVO>, List<AvProxyVO>>() {
                    @Override
                    public List<AvProxyVO> apply(List<AvProxyVO> input) {
                        // 注意Lists.newArrayList将会打断懒加载链条,如果有多个过滤条件,请在Lists.newArrayList之前完成
                        return Lists.newArrayList(Iterables.filter(input, new Predicate<AvProxyVO>() {
                            @Override
                            public boolean apply(AvProxyVO input) {
                                return !input.getCloud();// 过滤云代理,云代理不参与序列化
                            }
                        }));
                    }
                }));
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
