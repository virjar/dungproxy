package com.virjar.dungproxy.client.ningclient.proxyclient;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.ning.http.client.AsyncCompletionHandlerBase;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.Response;
import com.virjar.dungproxy.client.ningclient.http.HttpOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Description: ProxyClient
 *
 * @author lingtong.fu
 * @version 2016-09-07 11:40
 */
public class ProxyClient extends VirjarAsyncClient{

    private static final Logger log = LoggerFactory.getLogger(ProxyClient.class);

    public ProxyClient() {
        super();
    }

    public <T> ListenableFuture<T> get(final String url, HttpOption option, AsyncHandler<T> handler) throws IOException {

        ProxyOption realOption = ProxyOption.getRealOption(option);
        return privateGet(url, realOption, handler);
    }

    public <T> ListenableFuture<T> get(final String url, AsyncHandler<T> handler) throws IOException {
        return get(url, new HttpOption(), handler);
    }

    /**
     * 默认使用AsyncCompletionHandlerBase
     */
    public ListenableFuture<Response> getWithoutHandler(final String url) throws IOException {
        return get(url, new HttpOption(), new AsyncCompletionHandlerBase());

    }

    public ListenableFuture<Response> getWithoutHandler(final String url, HttpOption option) throws IOException {
        return get(url, option, new AsyncCompletionHandlerBase());
    }

    //返回值见Futures.successfulAsList的注释
    public <T> ListenableFuture<List<T>> get(final Map<String, AsyncHandler<T>> urlAndHandlers) {
        ListenableFuture[] futures = new ListenableFuture[urlAndHandlers.size()];
        int index = 0;
        for (Map.Entry<String, AsyncHandler<T>> asyncHandlerEntry : urlAndHandlers.entrySet()) {
            String url = asyncHandlerEntry.getKey();
            AsyncHandler<T> handler = asyncHandlerEntry.getValue();
            try {
                futures[index] = get(url, handler);
            } catch (Exception e) {
                futures[index] = Futures.immediateFailedFuture(e);
                log.error("invoke url error,url is {}", url, e);
            }
            index++;
        }

        return Futures.successfulAsList(futures);
    }

    public ListenableFuture<List<Response>> get(List<String> urls) {
        Map<String, AsyncHandler<Response>> maps = Maps.newHashMapWithExpectedSize(urls.size());
        for (String url : urls) {
            maps.put(url, new AsyncCompletionHandlerBase());
        }
        return get(maps);
    }

    public <T> ListenableFuture<T> post(final String url, Map<String, String> params,
                                        String charset, AsyncHandler<T> handler) throws IOException {
        return post(url, params, new HttpOption(), charset, handler);
    }

    public <T> ListenableFuture<T> post(final String url, Map<String, String> params, HttpOption option,
                                        String charset, AsyncHandler<T> handler) throws IOException {

        ProxyOption realOption = ProxyOption.getRealOption(option);
        return privatePost(url, params, realOption, charset, handler);
    }
}
