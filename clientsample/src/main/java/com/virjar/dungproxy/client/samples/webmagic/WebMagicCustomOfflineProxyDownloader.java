package com.virjar.dungproxy.client.samples.webmagic;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;

import com.virjar.dungproxy.client.webmagic.DungProxyDownloader;

import us.codecraft.webmagic.Page;

/**
 * Created by virjar on 17/1/13.
 */
public class WebMagicCustomOfflineProxyDownloader extends DungProxyDownloader {
    @Override
    protected boolean needOfflineProxy(Page page) {
        if (super.needOfflineProxy(page)) {// 父类默认下线 401和403
            return true;
        } else {
            return StringUtils.containsIgnoreCase(page.getRawText(), "包含这个关键字,代表IP被封禁");
        }
    }

    @Override
    protected boolean needOfflineProxy(IOException e) {
        // return e instanceof SSLException;//如果异常类型是SSL,代表IP被封禁,你也可以不实现
        return false;
    }

    @Override
    protected boolean needOfflineProxy(int status) {
        // 根据响应码下线IP,这里的响应码是webMagic里面不在acceptStatusCode里面的
        return false;
    }
}
