package com.virjar.dungproxy.client.sample;

import com.virjar.dungproxy.client.webmagic.DungProxyDownloader;
import org.apache.commons.lang3.StringUtils;
import us.codecraft.webmagic.Page;

/**
 * Created by virjar on 17/1/13.
 */
public class WebMagicCustomOfflineProxyDownloader extends DungProxyDownloader {
    @Override
    protected boolean needOfflineProxy(Page page) {
        if( super.needOfflineProxy(page)){//父类默认下线 401和403
            return true;
        }else{
            return StringUtils.containsIgnoreCase(page.getRawText(), "包含这个关键字,代表IP被封禁");
        }
    }
}
