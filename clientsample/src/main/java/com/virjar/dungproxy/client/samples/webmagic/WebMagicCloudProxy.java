package com.virjar.dungproxy.client.samples.webmagic;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.virjar.dungproxy.client.ippool.IpPoolHolder;
import com.virjar.dungproxy.client.ippool.config.DungProxyContext;
import com.virjar.dungproxy.client.ippool.strategy.impl.WhiteListProxyStrategy;
import com.virjar.dungproxy.client.model.AvProxyVO;
import com.virjar.dungproxy.client.samples.webmagic.dytt8.WebMagicTest;
import com.virjar.dungproxy.client.webmagic.DungProxyDownloader;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

/**
 * Created by virjar on 17/2/10.<br/>
 * webMagic & DungProxy & 阿布云三方混合使用 。dungproxy和abuyun同时提供代理服务,而且他们都会根据使用效果竞争代理机会使用权利
 */
public class WebMagicCloudProxy implements PageProcessor {
    private Site site = Site.me()// .setHttpProxy(new HttpHost("127.0.0.1",8888))
            .setRetryTimes(3) // 就我的经验,这个重试一般用处不大,他是httpclient内部重试
            .setTimeOut(30000)// 在使用代理的情况下,这个需要设置,可以考虑调大线程数目
            .setSleepTime(0)// 使用代理了之后,代理会通过切换IP来防止反扒。同时,使用代理本身qps降低了,所以这个可以小一些
            .setCycleRetryTimes(3)// 这个重试会换IP重试,是setRetryTimes的上一层的重试,不要怕三次重试解决一切问题。。
            .setUseGzip(true);

    public WebMagicCloudProxy() throws IOException {
    }

    public static void main(String[] args) throws IOException {
        // 以下是通过代码配置规则的方案,如果不使用配置文件,则可以解开注释,通过代码的方式

        WhiteListProxyStrategy whiteListProxyStrategy = new WhiteListProxyStrategy();
        whiteListProxyStrategy.addAllHost("www.dytt8.net,www.ygdy8.net");

        //云代理对象
        AvProxyVO avProxyVO = new AvProxyVO();
        avProxyVO.setIp("proxy.abuyun.com");//阿布云服务器地址
        avProxyVO.setPort(9010);//阿布云服务器端口
        avProxyVO.setCloud(true);//标示这个代理是云代理
        avProxyVO.setCloudCopyNumber(4);//个人阿布云账户是支持4个并发,使用者填写各自的并发数目
        //如果是通过头部认证的方式,则如下添加
        //avProxyVO.setAuthenticationHeaders(HeaderBuilder.create().withHeader("Proxy-Authorization", "token").buildList());
        avProxyVO.setUsername("H402MPRHB15K37YD");//代理帐户
        avProxyVO.setPassword("601AE248E2ABE744");//代理密码


        // Step2 创建并定制代理规则 DungProxyContext
        DungProxyContext dungProxyContext = DungProxyContext.create().setNeedProxyStrategy(whiteListProxyStrategy).addCloudProxy(avProxyVO); // Step3
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

                // try {
                // bufferedWriter.write(href);
                // bufferedWriter.newLine();
                // } catch (IOException e) {
                // e.printStackTrace();
                // }

                System.out.println(href);// 输出所有下下载地址
            } else {
                if (needAddToTarget(href)) {
                    page.addTargetRequest(href);
                }
            }
        }

        // try {
        // bufferedWriter.flush();
        // } catch (IOException e) {
        // e.printStackTrace();
        // }

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
