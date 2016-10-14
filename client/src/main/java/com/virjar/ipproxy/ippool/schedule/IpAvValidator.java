package com.virjar.ipproxy.ippool.schedule;

import java.io.IOException;

import com.virjar.ipproxy.httpclient.HttpInvoker;
import com.virjar.model.AvProxy;

/**
 * Description: 本地可用Proxy验证
 *
 * @author lingtong.fu
 * @version 2016-09-16 16:57
 */
public class IpAvValidator {
    public static boolean available(AvProxy avProxy, String testUrl) {
        for (int i = 0; i < 3; i++) {
            try {
                if (HttpInvoker.getStatus(testUrl, avProxy.getIp(), avProxy.getPort()) == 200) {
                    return true;
                }
            } catch (IOException e) {
                // do nothing
            }
        }
        return false;
    }
}
