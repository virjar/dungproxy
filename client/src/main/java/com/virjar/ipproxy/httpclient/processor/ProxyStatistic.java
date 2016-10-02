package com.virjar.ipproxy.httpclient.processor;

import java.io.IOException;

import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.protocol.HttpContext;

/**
 * 如果是和原生httpclient集成,需要将这个处理器放置到httpclientBuilder中,这样才能使得代理池有反馈下线的功能<br/>
 * Created by virjar on 16/10/1.
 */
public class ProxyStatistic implements HttpResponseInterceptor {
    @Override
    public void process(HttpResponse response, HttpContext context) throws HttpException, IOException {

    }
}
