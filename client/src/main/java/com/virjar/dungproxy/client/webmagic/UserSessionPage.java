package com.virjar.dungproxy.client.webmagic;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.virjar.dungproxy.client.ippool.config.ProxyConstant;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.utils.UrlUtils;

/**
 * Created by virjar on 17/2/16.
 */
public class UserSessionPage extends Page {


    @Override
    public void addTargetRequests(List<String> requests, final long priority) {
        for (String url : requests) {
            super.addTargetRequest(transform(url, priority));
        }
    }

    @Override
    public void addTargetRequest(Request request) {
        Object userID = getRequest().getExtra(ProxyConstant.DUNGPROXY_USER_KEY);
        if (userID != null) {
            request.putExtra(ProxyConstant.DUNGPROXY_USER_KEY, userID);
        }
        super.addTargetRequest(request);
    }

    @Override
    public void addTargetRequest(String requestString) {
        super.addTargetRequest(transform(requestString, 0));
    }

    @Override
    public void addTargetRequests(List<String> requests) {
        for (String url : requests) {
            super.addTargetRequest(transform(url, 0));
        }
    }

    private Request transform(String requestUrl, long priority) {
        if (StringUtils.isBlank(requestUrl) || requestUrl.equals("#") || requestUrl.startsWith("javascript:")) {
            return null;
        }
        requestUrl = UrlUtils.canonicalizeUrl(requestUrl, super.getUrl().toString());
        Request request = new Request(requestUrl).setPriority(priority);
        Object userID = getRequest().getExtra(ProxyConstant.DUNGPROXY_USER_KEY);
        if (userID != null) {
            request.putExtra(ProxyConstant.DUNGPROXY_USER_KEY, userID);
        }
        return request;
    }
}
