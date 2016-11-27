package com.virjar.dungproxy.server.crawler.impl;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.server.crawler.NewCollector;
import com.virjar.dungproxy.server.entity.Proxy;

/**
 * Created by virjar on 16/11/26.
 */
@Component
public class YouDailiCollector extends NewCollector {
    private static final Logger logger = LoggerFactory.getLogger(YouDailiCollector.class);
    private static Pattern ipAndPortPattern = Pattern.compile(
            "(([1-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3}):(\\d+)");
    private String seed = "http://www.youdaili.net/Daili/guonei/";
    private String lasUrl = seed;// 仅仅为了统计

    public YouDailiCollector() {
        setDuration(24 * 60);
    }

    @Override
    public String lasUrl() {
        return lasUrl;
    }

    @Override
    public List<Proxy> doCollect() {
        List<Proxy> ret = Lists.newArrayList();
        List<String> links = genLinks();
        if (links.size() == 0) {
            return ret;
        }
        for (String link : links) {
            ret.addAll(processLink(link));
        }
        return ret;
    }

    private List<Proxy> processLink(String url) {
        List<Proxy> ret = Lists.newArrayList();
        for (int i = 0; i < 3; i++) {
            try {
                String s = HttpInvoker.get(url);
                if (StringUtils.isEmpty(s)) {
                    continue;
                }
                Matcher matcher = ipAndPortPattern.matcher(s);
                while (matcher.find()) {
                    String ip = matcher.group(1);
                    Integer port = NumberUtils.toInt(matcher.group(4));
                    Proxy proxy = new Proxy();
                    proxy.setIp(ip);
                    proxy.setPort(port);
                    proxy.setConnectionScore(0L);
                    proxy.setAvailbelScore(0L);
                    proxy.setSource(lasUrl());
                    ret.add(proxy);
                }
                return ret;
            } catch (Exception e) {
                logger.error("解析有代理代理详情页异常", e);
            }
        }

        return ret;
    }

    private List<String> genLinks() {
        for (int i = 0; i < 3; i++) {
            try {
                String response = HttpInvoker.get(seed);
                Document document = Jsoup.parse(response);
                Elements allLinkElements = document.select(".chunlist ul li p a");
                List<String> allLinks = Lists.newArrayList();
                for (Element el : allLinkElements) {
                    allLinks.add(el.attr("href"));
                }
                return allLinks;
            } catch (Exception e) {
                logger.error("有代理原始链接解析异常", e);
            }
        }
        return Lists.newArrayList();
    }

    public static void main(String[] args) {
        List<Proxy> proxies = new YouDailiCollector().doCollect();
        System.out.println(JSONObject.toJSONString(proxies));
    }
}
