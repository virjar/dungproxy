package com.virjar.dungproxy;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.IOUtils;

import com.google.common.util.concurrent.ListenableFuture;
import com.ning.http.client.Response;
import com.virjar.dungproxy.client.ningclient.proxyclient.VirjarAsyncClient;

/**
 * Created by virjar on 17/1/31.
 */
public class NingHttpclientTest {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        VirjarAsyncClient virjarAsyncClient = new VirjarAsyncClient();
        ListenableFuture<Response> listenableFuture = virjarAsyncClient.get("http://www.baidu.com");
        Response response = listenableFuture.get();
        System.out.println(IOUtils.toString(response.getResponseBodyAsStream()));
    }
}
