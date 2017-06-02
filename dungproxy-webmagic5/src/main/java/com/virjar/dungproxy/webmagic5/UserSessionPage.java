package com.virjar.dungproxy.webmagic5;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.virjar.dungproxy.client.ippool.config.ProxyConstant;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Request;
import us.codecraft.webmagic.utils.UrlUtils;

/**
 * Created by virjar on 17/2/16.<br/>
 * 重写page里面的方法,对用户URL关系维护做增强,也就是说纪录增加的URL任务是那个user产生的,便于新URL任务执行的时候,选择合适的cookie空间,做到多用户模拟登录cookie隔离
 */
public class UserSessionPage extends Page {

    @Override
    public void addTargetRequests(List<String> requests, final long priority) {
        for (String url : requests) {
            addTargetRequest(transform(url, priority));
        }
    }

    @Override
    public void addTargetRequest(Request request) {
        if (request == null) {
            return;
        }
        Object userID = getRequest().getExtra(ProxyConstant.DUNGPROXY_USER_KEY);
        if (userID != null) {
            request.putExtra(ProxyConstant.DUNGPROXY_USER_KEY, userID);
        }
        super.addTargetRequest(request);
    }

    @Override
    public void addTargetRequest(String requestString) {
        addTargetRequest(transform(requestString, 0));
    }

    @Override
    public void addTargetRequests(List<String> requests) {
        for (String url : requests) {
            addTargetRequest(transform(url, 0));
        }
    }

    private Request transform(String requestUrl, long priority) {
        if (StringUtils.isBlank(requestUrl) || requestUrl.equals("#") || requestUrl.startsWith("javascript:")) {
            return null;
        }
        requestUrl = UrlUtils.canonicalizeUrl(requestUrl, super.getUrl().toString());
        return new Request(requestUrl).setPriority(priority);

    }
}
