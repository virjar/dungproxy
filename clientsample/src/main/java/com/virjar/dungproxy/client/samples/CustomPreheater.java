package com.virjar.dungproxy.client.samples;

import org.apache.http.Header;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.virjar.dungproxy.client.httpclient.HeaderBuilder;
import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.client.ippool.PreHeater;
import com.virjar.dungproxy.client.ippool.config.DungProxyContext;
import com.virjar.dungproxy.client.ippool.strategy.ProxyChecker;
import com.virjar.dungproxy.client.model.AvProxyVO;

/**
 * Created by virjar on 17/4/28.
 */
public class CustomPreheater implements ProxyChecker {

    public static void main(String[] args) {
        DungProxyContext dungProxyContext = DungProxyContext.create().setDefaultProxyChecker(CustomPreheater.class)
                .buildDefaultConfigFile().handleConfig();

        PreHeater preHeater = dungProxyContext.getPreHeater();
        preHeater.doPreHeat();
        preHeater.destroy();
    }

    @Override
    public boolean available(AvProxyVO avProxyVO, String url) {
        Header[] headers = HeaderBuilder.create().withRefer("http://www.itslaw.com").buildArray();
        for (int i = 0; i < 3; i++) {
            try {
                String s = HttpInvoker.get(
                        "http://www.itslaw.com/api/v1/detail?judgementId=cdb60091-f0aa-484a-9cb2-494a8c6be460",
                        headers);
                JSONObject jsonObject = JSON.parseObject(s);
                if (jsonObject.getJSONObject("result").getInteger("code") == 0) {
                    return true;
                }
            } catch (Exception e) {
                // do nothing
            }
        }
        return false;
    }
}
