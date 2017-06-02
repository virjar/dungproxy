package com.virjar.dungproxy.client.samples.webmagic;

import com.virjar.dungproxy.client.ippool.IpPoolHolder;
import com.virjar.dungproxy.client.ippool.config.DungProxyContext;
import com.virjar.dungproxy.client.ippool.strategy.impl.JSONFileAvProxyDumper;
import com.virjar.dungproxy.client.ippool.strategy.impl.WhiteListProxyStrategy;
import com.virjar.dungproxy.client.webmagic.DungProxyDownloader;

import us.codecraft.webmagic.Page;
import us.codecraft.webmagic.Site;
import us.codecraft.webmagic.Spider;
import us.codecraft.webmagic.processor.PageProcessor;

/**
 * Created by virjar on 17/2/20.
 */
public class TianyaTest implements PageProcessor {
    private static Site site = Site.me()// .setHttpProxy(new HttpHost("127.0.0.1",8888))
            .setRetryTimes(3) // 就我的经验,这个重试一般用处不大,他是httpclient内部重试
            .setTimeOut(30000)// 在使用代理的情况下,这个需要设置,可以考虑调大线程数目
            .setSleepTime(0)// 使用代理了之后,代理会通过切换IP来防止反扒。同时,使用代理本身qps降低了,所以这个可以小一些
            .setCycleRetryTimes(3)// 这个重试会换IP重试,是setRetryTimes的上一层的重试,不要怕三次重试解决一切问题。。
            .setUseGzip(true);

    public static void main(String[] args) {
        WhiteListProxyStrategy whiteListProxyStrategy = new WhiteListProxyStrategy();
        whiteListProxyStrategy.addAllHost("www.tianya.cn");
        JSONFileAvProxyDumper jsonFileAvProxyDumper = new JSONFileAvProxyDumper();
        jsonFileAvProxyDumper.setDumpFileName("availableProxy.json");
        DungProxyContext dungProxyContext = DungProxyContext.create().setNeedProxyStrategy(whiteListProxyStrategy)
                .setAvProxyDumper(jsonFileAvProxyDumper).setPoolEnabled(true);
        dungProxyContext.getGroupBindRouter().buildCombinationRule("www.tianya.cn:.*tianya.*");
        IpPoolHolder.init(dungProxyContext);

        Spider.create(new TianyaTest())
                .addUrl("http://search.tianya.cn/bbs?q=%E5%9F%8B%E7%BA%BF%E5%8F%8C%E7%9C%BC%E7%9A%AE")
                .addUrl("http://search.tianya.cn/bbs?q=%E5%BE%AE%E5%88%9B%E5%8F%8C%E7%9C%BC%E7%9A%AE")
                .addUrl("http://bbs.tianya.cn/post-funinfo-5695400-1.shtml").setDownloader(new DungProxyDownloader())
                .thread(1).run();
    }

    @Override
    public void process(Page page) {

    }

    @Override
    public Site getSite() {
        return site;
    }
}
