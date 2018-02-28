package com.virjar.dungproxy.client.over_webcollector;

import cn.edu.hfut.dmic.webcollector.crawler.AutoParseCrawler;
import cn.edu.hfut.dmic.webcollector.model.CrawlDatum;
import cn.edu.hfut.dmic.webcollector.model.Page;

/**
 * 为webCollector实现的自动代理适配器,如有其他需求,可以参考本来重新实现<br/>
 * Created by virjar on 16/10/30.<br/>
 * 继承本类即可实现自动代理切换。对于webCollector不支持根据user绑定IP的功能,智能随即选择IP,用户如果如果需要,可以考虑自己实现
 *
 */
public abstract class DungProxyAutoParserCrawler extends AutoParseCrawler {
    public DungProxyAutoParserCrawler(boolean autoParse) {
        super(autoParse);
    }

    @Override
    public Page getResponse(CrawlDatum crawlDatum) throws Exception {
        DungproxyHttpRequest request = new DungproxyHttpRequest(crawlDatum);
        return request.responsePage();
    }

}