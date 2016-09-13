package com.virjar.ippool.schedule;

import com.google.common.collect.Maps;
import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import com.virjar.client.http.HttpOption;
import com.virjar.client.proxyclient.VirjarAsyncClient;
import com.virjar.model.AvProxy;
import com.virjar.utils.JSONUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Description: 初始化时加载Proxy
 *
 * @author lingtong.fu
 * @version 2016-09-11 18:16
 */
public class Preheater {

    private static VirjarAsyncClient client = new VirjarAsyncClient();

    private static final String url = "http://115.159.40.202:8080/proxyipcenter/av";

    private static volatile Map<Long, AvProxy> ID_PROXY_MAP = Maps.newHashMap();

    public void run() {
        initMaps();
    }

    private void initMaps() {
        try {
            List<AvProxy> avProxies = JSONUtils.parseList(getFuture(url).get(60000, TimeUnit.MILLISECONDS), AvProxy.class);
            Map<Long, AvProxy> map = Maps.newHashMap();
            assert avProxies != null;
            for (AvProxy avProxy : avProxies) {
                map.put(avProxy.getId(), avProxy);
            }
            ID_PROXY_MAP = map;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Future<String> getFuture(String url) throws IOException {
        HttpOption httpOption = new HttpOption();
        httpOption.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        return client.get(url, httpOption, new AsyncCompletionHandler<String>() {
            @Override
            public String onCompleted(Response response) throws Exception {
                if (response.getStatusCode() == 200) {
                    return response.getResponseBody();
                }
                return null;
            }
        });
    }
}
