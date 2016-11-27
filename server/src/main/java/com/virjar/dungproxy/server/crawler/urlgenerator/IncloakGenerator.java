package com.virjar.dungproxy.server.crawler.urlgenerator;

import com.virjar.dungproxy.server.crawler.URLGenerator;

/**
 * Created by virjar on 16/11/26.
 */
public class IncloakGenerator extends URLGenerator {
    int pageSize = 64;

    public IncloakGenerator() {
        totallPage = 12;
    }

    @Override
    public String newURL() {
        if (nowPage > totallPage) {
            reset();
        }
        String url = "https://incloak.com/proxy-list/?start=" + ((pageSize-1) * nowPage) + "#list";
        nowPage++;
        return url;
    }
}
