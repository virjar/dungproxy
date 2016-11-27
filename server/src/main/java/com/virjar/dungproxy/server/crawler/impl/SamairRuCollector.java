package com.virjar.dungproxy.server.crawler.impl;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;
import com.virjar.dungproxy.server.crawler.AutoDownloadCollector;
import com.virjar.dungproxy.server.entity.Proxy;

/**
 * Created by virjar on 16/11/27.
 */
@Component
public class SamairRuCollector extends AutoDownloadCollector {
    private static Pattern ipAndPortPattern = Pattern.compile(
            "(([1-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3}):(\\d+)");

    public SamairRuCollector() {
        setDuration(120);
    }

    @Override
    protected List<Proxy> parse(String response) {
        List<Proxy> ret = Lists.newArrayList();
        Matcher matcher = ipAndPortPattern.matcher(response);
        while (matcher.find()) {
            Proxy proxy = new Proxy();
            proxy.setIp(matcher.group(1));
            proxy.setPort(NumberUtils.toInt(matcher.group(4)));
            proxy.setAvailbelScore(0L);
            proxy.setConnectionScore(0L);
            proxy.setSource(lasUrl());
            ret.add(proxy);
        }
        return ret;
    }

    @Override
    protected String newUrl() {
        return "http://samair.ru/proxy/list-IP-port/proxy-" + (pageNow - 1) + ".htm";
    }
}
