package com.virjar.util;

/**
 *
 * Created by lingtong.fu on 2016/8/30.
 */

import com.virjar.model.Proxy;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;


public class IpPoolFactory implements PooledObjectFactory<Proxy> {

    public PooledObject<Proxy> makeObject() throws Exception {
        System.out.println("make Object");
        Proxy proxy = new Proxy();
        return new DefaultPooledObject<Proxy>(proxy);
    }

    public void destroyObject(PooledObject<Proxy> pooledObject) throws Exception {
        System.out.println("destroy Object");
        Proxy proxy = pooledObject.getObject();
        proxy = null;
    }

    public boolean validateObject(PooledObject<Proxy> pooledObject) {
        System.out.println("validate Object");
        return true;
    }

    public void activateObject(PooledObject<Proxy> pooledObject) throws Exception {
        System.out.println("activate Object");
    }

    public void passivateObject(PooledObject<Proxy> pooledObject) throws Exception {
        System.out.println("passivate Object");
    }
}