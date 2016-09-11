package com.virjar.ippool;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;

/**
 * Description: IpPoolConfig
 *
 * @author lingtong.fu
 * @version 2016-09-11 17:02
 */
public class IpPoolConfig extends GenericObjectPoolConfig {

    public IpPoolConfig(){
        //setMinIdle(3);
        //setMaxIdle(8);
        //setTestOnBorrow(true);
    }
}
