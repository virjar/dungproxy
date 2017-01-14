package com.virjar.dungproxy.client;

import org.apache.commons.lang.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.virjar.dungproxy.client.webmagic.DungProxyDownloader;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

/**
 * Created by virjar on 17/1/14.<br/>
 * webMagic测试,使用webMagic爬取电影天堂的电影文件下载地址
 *
 */
public class WebMagicTest implements PageProcessor {
    private Site site = Site.me()// .setHttpProxy(new HttpHost("127.0.0.1",8888))
            .setRetryTimes(3).setTimeOut(30000).setSleepTime(0).setUseGzip(true);//注意调整超时时间

    public static void main(String[] args) {
        Spider.create(new WebMagicTest()).setDownloader(new DungProxyDownloader()).thread(30)
                .addUrl("http://www.dytt8.net/index.html")
                .addUrl("http://www.ygdy8.net/html/gndy/dyzz/index.html")
                .addUrl("http://www.dytt8.net/html/gndy/index.html").run();

    }

    @Override
    public void process(Page page) {
        Elements a = page.getHtml().getDocument().getElementsByTag("a");
        for (Element el : a) {
            String href = el.absUrl("href");
            if (StringUtils.startsWith(href, "ftp:")) {
                System.out.println(href);//输出所有下下载地址
            } else {
                if (needAddToTarget(href)) {
                    page.addTargetRequest(href);
                }
            }
        }

    }

    private boolean needAddToTarget(String url) {
        if (url.endsWith(".png") || url.endsWith(".jpg") || url.endsWith(".gif")) {
            return false;
        }

        return url.contains("www.dytt8.net") || url.contains("www.ygdy8.net");
    }

    @Override
    public Site getSite() {
        return site;
    }
}
