import org.apache.commons.lang3.StringUtils;

import com.virjar.dungproxy.client.model.AvProxy;
import com.virjar.dungproxy.webmagic7.DungProxyProvider;
import com.virjar.dungproxy.webmagic7.OfflineStrategy;

import us.codecraft.webmagic.Page;

/**
 * Created by virjar on 17/6/3.
 */
public class PrividerTest {
    public static void main(String[] args) {
        // 默认不下线的case,但是会记录失败
        DungProxyProvider dungProxyProvider = new DungProxyProvider("www.java1234.com", "http://www.java1234.com");

        // 包含某个关键字,代表IP被封禁
        dungProxyProvider = new DungProxyProvider("www.java1234.com", "http://www.java1234.com", new OfflineStrategy() {
            @Override
            public boolean needOfflineProxy(Page page, AvProxy avProxy) {
                return !page.isDownloadSuccess() && StringUtils.contains(page.getRawText(), "对不起,你的IP暂时不能访问此网页");
            }
        });

        // 包含某个关键字,不下线IP,但是暂时封禁IP,一段时间可以重新使用
        dungProxyProvider = new DungProxyProvider("www.java1234.com", "http://www.java1234.com", new OfflineStrategy() {
            @Override
            public boolean needOfflineProxy(Page page, AvProxy avProxy) {
                if (!page.isDownloadSuccess() && StringUtils.contains(page.getRawText(), "对不起,你的IP暂时不能访问此网页")) {
                    avProxy.block(2 * 60 * 60 * 1000);
                }
                return false;
            }
        });

    }
}
