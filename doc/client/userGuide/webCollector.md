
### 和webCollector的集成
webCollector是国内另一个比较流行的java爬虫框架,我也对他提供直接支持。方式如下:
继承``com.virjar.dungproxy.client.webcollector.DungProxyAutoParserCrawler``
```
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
```