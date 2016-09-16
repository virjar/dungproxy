package com.virjar.ippool;

/**
 *
 * Created by lingtong.fu on 2016/8/30.
 */

import com.virjar.ippool.schedule.Preheater;
import com.virjar.model.AvProxy;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import javax.annotation.Resource;


public class IpPooledObjectFactory extends BasePooledObjectFactory<AvProxy> {

    @Resource
    private Preheater preheater;

    @Override
    public AvProxy create() throws Exception {
        return new AvProxy();
    }

    @Override
    public PooledObject<AvProxy> wrap(AvProxy obj) {
        return new DefaultPooledObject<AvProxy>(obj);
    }

    public PooledObject<AvProxy> makeObject() throws Exception {
        System.out.println("make null Object");
        //TODO 本地可用Proxy验证
        AvProxy AvProxy = preheater.getAvProxy();
        return wrap(AvProxy);
    }

    public void destroyObject(PooledObject<AvProxy> pooledObject) throws Exception {
        System.out.println("destroy Object");
        AvProxy AvProxy = pooledObject.getObject();
        AvProxy = null;
    }

    public boolean validateObject(PooledObject<AvProxy> pooledObject) {
        System.out.println("validate Object");
        return true;
    }

    public void activateObject(PooledObject<AvProxy> pooledObject) throws Exception {
        System.out.println("activate Object");
    }

    public void passivateObject(PooledObject<AvProxy> pooledObject) throws Exception {
        System.out.println("passivate Object");
    }

}