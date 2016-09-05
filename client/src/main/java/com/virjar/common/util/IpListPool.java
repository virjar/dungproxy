package com.virjar.common.util;

import com.virjar.model.Proxy;
import org.apache.commons.pool2.BaseObjectPool;

/**
 * Description: IpListPool
 *
 * @author lingtong.fu
 * @version 2016-09-03 01:13
 */
public class IpListPool extends BaseObjectPool<Proxy>{
    @Override
    public Proxy borrowObject() throws Exception {
        return null;
    }

    @Override
    public void returnObject(Proxy obj) throws Exception {

    }

    @Override
    public void invalidateObject(Proxy obj) throws Exception {

    }
}
