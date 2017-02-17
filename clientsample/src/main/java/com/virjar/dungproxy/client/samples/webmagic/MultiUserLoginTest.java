package com.virjar.dungproxy.client.samples.webmagic;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.protocol.HttpClientContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.collect.Maps;
import com.virjar.dungproxy.client.httpclient.CrawlerHttpClient;
import com.virjar.dungproxy.client.httpclient.HeaderBuilder;
import com.virjar.dungproxy.client.httpclient.NameValuePairBuilder;
import com.virjar.dungproxy.client.ippool.IpPoolHolder;
import com.virjar.dungproxy.client.ippool.config.DungProxyContext;
import com.virjar.dungproxy.client.ippool.config.ProxyConstant;
import com.virjar.dungproxy.client.util.CommonUtil;
import com.virjar.dungproxy.client.util.PoolUtil;
import com.virjar.dungproxy.client.webmagic.DungProxyDownloader;

import us.codecraft.webmagic.*;
import us.codecraft.webmagic.processor.PageProcessor;
import us.codecraft.webmagic.scheduler.Scheduler;

/**
 * Created by virjar on 17/2/17.<br/>
 * 测试对多用户登录的支持
 */
public class MultiUserLoginTest implements PageProcessor {
    private static Site site = Site.me()// .setHttpProxy(new HttpHost("127.0.0.1",8888))
            .setRetryTimes(3) // 就我的经验,这个重试一般用处不大,他是httpclient内部重试
            .setTimeOut(30000)// 在使用代理的情况下,这个需要设置,可以考虑调大线程数目
            .setSleepTime(0)// 使用代理了之后,代理会通过切换IP来防止反扒。同时,使用代理本身qps降低了,所以这个可以小一些
            .setCycleRetryTimes(3)// 这个重试会换IP重试,是setRetryTimes的上一层的重试,不要怕三次重试解决一切问题。。
            .setUseGzip(true);
    private static CrawlerHttpClient httpclient = null;
    private static Random random = new Random();

    private static Map<String, OSChinaUser> userMap = Maps.newConcurrentMap();

    static {
        OSChinaUser osChinaUser = new OSChinaUser();
        osChinaUser.account = "test1@virjar.com";
        osChinaUser.password = "ylUbg73Qs";
        osChinaUser.userName = "维佳1号";
        userMap.put(osChinaUser.account, osChinaUser);

        osChinaUser = new OSChinaUser();
        osChinaUser.account = "test2@virjar.com";
        osChinaUser.password = "ylUbg73Qs";
        osChinaUser.userName = "维佳2号";
        userMap.put(osChinaUser.account, osChinaUser);

        osChinaUser = new OSChinaUser();
        osChinaUser.account = "test3@virjar.com";
        osChinaUser.password = "ylUbg73Qs";
        osChinaUser.userName = "维佳3号";
        userMap.put(osChinaUser.account, osChinaUser);
    }

    public static void main(String[] args) {

        // STEP 0 默认的代理池,可选方案,代理池的具体使用参考文档。这里禁用dungproxy的代理池功能
        ProxyConstant.CLIENT_CONFIG_FILE_NAME = "proxyclient_oschina.properties";
        DungProxyContext dungProxyContext = DungProxyContext.create().setPoolEnabled(false);
        IpPoolHolder.init(dungProxyContext);

        // STEP 1 获取原生的httpclient执行登录操作
        DungProxyDownloader dungProxyDownloader = new DungProxyDownloader();
        CrawlerHttpClient httpClient = dungProxyDownloader.getHttpClient(site, null);
        MultiUserLoginTest.httpclient = httpClient;

        // STEP 2 装配webMagic
        Spider spider = Spider.create(new MultiUserLoginTest()).setScheduler(new Scheduler() {
            // 重写Scheduler,为了让种子使用随机的方式调度URL任务,否则大部分URL都是第一个登录的账户的。
            // 这个调度器让任务根据用户来做均衡。不是这个始终是demo,实际上的用法视情况而定
            // 请注意这个没有重写消重器,因为涉及多个账户数据,可能同一个URL的数据内容也不同,所以消重规则应该视情况而定
            private ConcurrentMap<String, ConcurrentLinkedDeque<Request>> data = Maps.newConcurrentMap();

            private ConcurrentLinkedDeque<Request> createOrGet(Request request) {
                Object extra = request.getExtra(ProxyConstant.DUNGPROXY_USER_KEY);
                if (extra == null) {
                    extra = "default_user_account";
                }
                ConcurrentLinkedDeque<Request> requests = data.get(extra.toString());
                if (requests == null) {
                    synchronized (MultiUserLoginTest.class) {
                        requests = data.get(extra.toString());
                        if (requests == null) {
                            data.put(extra.toString(), new ConcurrentLinkedDeque<Request>());
                            requests = data.get(extra.toString());
                        }
                    }
                }
                return requests;
            }

            @Override
            public void push(Request request, Task task) {
                createOrGet(request).add(request);
            }

            @Override
            public Request poll(Task task) {
                int i = random.nextInt(data.size());
                for (ConcurrentLinkedDeque<Request> queque : data.values()) {
                    if (i == 0) {
                        Request request = queque.poll();
                        if (request != null) {
                            return request;
                        }
                    }
                    i--;
                }
                for (ConcurrentLinkedDeque<Request> queque : data.values()) {
                    Request request = queque.poll();
                    if (request != null) {
                        return request;
                    }

                }
                return null;
            }
        }).setDownloader(dungProxyDownloader).thread(1);

        // STEP 3 登录操作
        for (OSChinaUser user : userMap.values()) {
            if (login(httpClient, user)) {// 登录成功后为这个用户添加一个种子
                Request request = new Request("https://www.oschina.net/?nocache=" + System.currentTimeMillis());
                request.putExtra(ProxyConstant.DUNGPROXY_USER_KEY, user.account);// 绑定账户到种子上面
                spider.addRequest(request);
                CommonUtil.sleep(10);// 为了让时间戳不同,避免URL被webmagic消重机制给干掉,实际上关于URL消重应该好好考虑如何实现了
            }
        }

        // STEP 4开启爬虫
        spider.run();

    }

    private static class OSChinaUser {
        String account;
        String password;
        String userName;
    }

    private static boolean login(CrawlerHttpClient crawlerHttpClient, OSChinaUser user) {
        if (user == null) {
            return false;
        }
        HttpClientContext httpClientContext = HttpClientContext.create();
        PoolUtil.bindUserKey(httpClientContext, user.account);// 绑定账户到这个请求
        PoolUtil.disableDungProxy(httpClientContext);// 暂时禁用代理功能。如果你的代理比较快,可以不禁用

        // 构造登录表单参数
        List<NameValuePair> params = NameValuePairBuilder.create().addParam("email", user.account)
                .addParam("pwd", DigestUtils.sha1Hex(user.password)).addParam("verifyCode").addParam("save_login", "1")
                .build();

        Header[] headers = HeaderBuilder.create().defaultCommonHeader().withRefer("https://www.oschina.net/home/login")
                .buildArray();

        // 登录操作
        crawlerHttpClient.post("https://www.oschina.net/action/user/hash_login", params, headers, httpClientContext);
        String s = crawlerHttpClient.get("https://www.oschina.net/?nocache=" + System.currentTimeMillis(),
                httpClientContext);

        if (StringUtils.contains(s, "/action/user/logout")) {// 有退出登录的链接,代表登录成功
            Elements select = Jsoup.parse(s).select(".user-info span[class=name]");
            String loginUserName = select.first().text();
            System.out.println(loginUserName + "登录成功");
            return true;
        }
        return false;
    }

    @Override
    public void process(Page page) {
        List<String> allLinks = page.getHtml().links().all();

        // 绑定在URL种子里面的账户
        Object account = page.getRequest().getExtra(ProxyConstant.DUNGPROXY_USER_KEY);
        String urlUser = null;
        if (account != null) {
            urlUser = account.toString();
        }
        // xsoup有bug,,,,
        // String user = page.getHtml().css(".user-info span[class=name]").xpath("text()").get();
        Element first = page.getHtml().getDocument().select(".user-info span[class=name").first();
        String userName = "不知道姓名";
        if (first != null) {
            userName = first.ownText().trim();// 这是显示名称,URL种子里面保存的是账户
            if (urlUser != null && !StringUtils.equalsIgnoreCase(userMap.get(urlUser).userName, userName)) {
                // 这句话永远不能打印出来,如果打印出来,就证明多用户的功能支持不正常
                System.out.println("这个链接是:" + urlUser + " 抓取到的,但是返回的网页却是:" + userName + " 的名字,cookie紊乱");
            }
        } else {
            // cookie失效,重新登录
            if (StringUtils.contains(page.getRawText(), "https://www.oschina.net/home/reg")) {
                if (account != null) {
                    String userText = account.toString();
                    System.out.println("用户" + userText + ":session失效,重新登录...");
                    login(httpclient, userMap.get(userText));
                }
            }
        }
        System.out.println("当前页面是:《" + userName + "》 的数据");
        for (String url : allLinks) {
            if (StringUtils.contains(url, "my.oschina.net")) {// 控制爬虫在自己家目录爬取,实际上根据自己需要控制爬虫边缘
                page.addTargetRequest(url);
            }
        }
    }

    @Override
    public Site getSite() {
        return site;
    }
}
