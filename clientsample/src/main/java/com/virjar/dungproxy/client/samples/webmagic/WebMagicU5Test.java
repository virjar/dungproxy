package com.virjar.dungproxy.client.samples.webmagic;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.virjar.dungproxy.client.ippool.IpPoolHolder;
import com.virjar.dungproxy.client.ippool.config.DungProxyContext;
import com.virjar.dungproxy.client.samples.poolstrategy.U5IpSource;
import com.virjar.dungproxy.client.samples.webmagic.dytt8.WebMagicTest;
import com.virjar.dungproxy.client.webmagic.DungProxyDownloader;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

/**
 * Created by virjar on 17/2/14.<br/>
 * 无忧代理和接入dungproxy,使用webmagic做爬虫<br/>
 * www.data5u.com 很悲伤的是,此路基本不通,IP基本不可用
 */
public class WebMagicU5Test implements PageProcessor {
    private Site site = Site.me()// .setHttpProxy(new HttpHost("127.0.0.1",8888))
            .setRetryTimes(3) // 就我的经验,这个重试一般用处不大,他是httpclient内部重试
            .setTimeOut(30000)// 在使用代理的情况下,这个需要设置,可以考虑调大线程数目
            .setSleepTime(0)// 使用代理了之后,代理会通过切换IP来防止反扒。同时,使用代理本身qps降低了,所以这个可以小一些
            .setCycleRetryTimes(3)// 这个重试会换IP重试,是setRetryTimes的上一层的重试,不要怕三次重试解决一切问题。。
            .setUseGzip(true);

    public static void main(String[] args) throws IOException {

        DungProxyContext dungProxyContext = DungProxyContext.create()
                .buildDefaultConfigFile()//加这样一句
                .setDefaultCoreSize(1)
                .setDefaultResourceFacade(U5IpSource.class); // Step3
        // Step 3 使用代理规则构造默认IP池
        IpPoolHolder.init(dungProxyContext);

        Spider.create(new WebMagicTest()).setDownloader(new DungProxyDownloader()).thread(1)
                .addUrl("http://www.dytt8.net/index.html").addUrl("http://www.ygdy8.net/html/gndy/dyzz/index.html")
                .addUrl("http://www.dytt8.net/html/gndy/index.html").run();

    }

    @Override
    public void process(Page page) {
        Elements a = page.getHtml().getDocument().getElementsByTag("a");
        for (Element el : a) {
            String href = el.absUrl("href");
            if (StringUtils.startsWith(href, "ftp:") || StringUtils.endsWith(href, ".rar")) {

                System.out.println(href);// 输出所有下下载地址
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
