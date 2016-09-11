package com.virjar.ippool;

/**
 *
 * Created by lingtong.fu on 2016/8/30.
 */

import com.google.common.collect.Lists;
import com.virjar.model.AvProxy;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;

import java.util.List;

public class IpPooledObjectFactory implements PooledObjectFactory<AvProxy> {

    public PooledObject<AvProxy> makeObject() throws Exception {
        System.out.println("make Object");
        AvProxy AvProxy = new AvProxy();
        return new DefaultPooledObject<AvProxy>(AvProxy);
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