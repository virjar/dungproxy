package com.virjar;

import com.virjar.ipproxy.ippool.IpPool;
import com.virjar.ipproxy.ippool.IpPoolConfig;
import com.virjar.ipproxy.ippool.IpPooledObjectFactory;
import com.virjar.model.AvProxy;
import org.junit.Test;

/**
 * Description: PoolTest
 *
 * @author lingtong.fu
 * @version 2016-09-11 16:37
 */
public class PoolTest {

    @Test
    public void test() throws Exception{
        /*IpPoolConfig ipPoolConfig = new IpPoolConfig();
        IpPooledObjectFactory ipPooledObjectFactory = new IpPooledObjectFactory();
        IpPool ipPool = new IpPool(ipPooledObjectFactory, ipPoolConfig);

        AvProxy avProxy1 = ipPool.borrowObject();
        AvProxy avProxy2 = ipPool.borrowObject();
        AvProxy avProxy3 = ipPool.borrowObject();
        AvProxy avProxy4 = ipPool.borrowObject();
        AvProxy avProxy5 = ipPool.borrowObject();

        avProxy1.toString();
        ipPool.returnObject(avProxy1);
        avProxy2.toString();
        ipPool.returnObject(avProxy2);
        avProxy3.toString();
        ipPool.returnObject(avProxy3);
        avProxy4.toString();
        ipPool.returnObject(avProxy4);
        avProxy5.toString();
        ipPool.returnObject(avProxy5);

        ipPooledObjectFactory.makeObject();
        ipPool.addObject();*/
    }
}
