package com.virjar.dungproxy.client;

import com.virjar.dungproxy.client.webcollector.DungProxyAutoParserCrawler;

import cn.edu.hfut.dmic.webcollector.model.CrawlDatums;
import cn.edu.hfut.dmic.webcollector.model.Page;

/**
 * Created by virjar on 16/10/31.
 */
public class WebColletorTest extends DungProxyAutoParserCrawler {
    public WebColletorTest(boolean autoParse) {
        super(autoParse);
        this.addSeed("http://www.66ip.cn/2.html");
    }

    public static void main(String[] args) throws Exception {
        new WebColletorTest(true).start(10);
    }

    @Override
    public void visit(Page page, CrawlDatums next) {
        // 页面处理逻辑
    }
}
