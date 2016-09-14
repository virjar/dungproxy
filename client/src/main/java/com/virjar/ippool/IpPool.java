package com.virjar.ippool;

import com.virjar.model.AvProxy;
import org.apache.commons.pool2.BaseObjectPool;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.AbandonedConfig;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * Description: IpListPool
 *
 * @author lingtong.fu
 * @version 2016-09-03 01:13
 */
public class IpPool extends GenericObjectPool<AvProxy> {

    public IpPool(PooledObjectFactory<AvProxy> factory) {
        super(factory);
    }

    public IpPool(PooledObjectFactory<AvProxy> factory, GenericObjectPoolConfig config) {
        super(factory, config);
    }

    public IpPool(PooledObjectFactory<AvProxy> factory, GenericObjectPoolConfig config, AbandonedConfig abandonedConfig) {
        super(factory, config, abandonedConfig);
    }
}
