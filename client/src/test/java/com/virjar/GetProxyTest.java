package com.virjar;

import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import com.virjar.client.proxyclient.VirjarAsyncClient;
import com.virjar.entity.Proxy;
import com.virjar.model.Phone;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * Description: getProxyTest
 *
 * @author lingtong.fu
 * @version 2016-09-09 20:47
 */
public class GetProxyTest {

    private static VirjarAsyncClient client = new VirjarAsyncClient();

    private static final String url = "http://115.159.40.202:8080/proxyipcenter/av";

    private static final String baiduurl = "https://www.baidu.com/";

    public static void main(String[] args) {
        GetProxyTest getProxyTest = new GetProxyTest();

        System.out.println(getProxyTest.getAv(baiduurl));
    }

    public  String getAv(String url) {
        String s = null;
        try {
            s = getFuture(url).get(60000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }

    public static Future<String> getFuture(String url) throws IOException {

        return client.get(url, new AsyncCompletionHandler<String>() {
            @Override
            public String onCompleted(Response response) throws Exception {
                if (response.getStatusCode() == 200) {
                    return response.getResponseBody();
                }
                return null;
            }
        });
    }

    private static Proxy parseProxy(String responseBody) {
        return null;
    }
}
