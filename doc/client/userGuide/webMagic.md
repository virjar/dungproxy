### 多版本webMagic兼容
dungproxy自0.0.7之后,移除再core中对webmagic的依赖,提供三个分支项目(dungproxy-webmagic5,dungproxy-webmagic6,dungproxy-webmagic7)
分别对应webmagic0.5.x 0.6.x 0.7.x的适配,其主要方式是提供downloader。对于dungproxy-webmagic7,根据webmagic原生IP池接口做了实现。如下:
com.virjar.dungproxy.webmagic7.DungProxyProvider
```
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
```

DungProxyProvider 可以有三个参数,host,testurl,OfflineStrategy,其中host用于区分代理池的目标网站,testurl让代理池可以探测IP可用性,OfflineStrategy则可以自定义IP下线策略、IP封禁策略、IP评分控制、IP临时封禁等

请注意使用DungProxyProvider,和 DungProxyDownloader 的方式不一样,他是走webmagic原始逻辑,再httpclient上层实现故不能实现每次请求拦截IP可用性,如果像让IP使用情况精确反应到IP评分上,建议使用DungProxyDownloader

三个依赖分别的maven坐标
```
<!-- 适用于0.5.3 -->
<dependency>
  <groupId>com.virjar</groupId>
  <artifactId>dungproxy-webmagic5</artifactId>
  <version>0.0.1</version>
</dependency>

<!-- 适用于0.6.1 -->
<dependency>
  <groupId>com.virjar</groupId>
  <artifactId>dungproxy-webmagic6</artifactId>
  <version>0.0.1</version>
</dependency>

<!-- 适用于0.7.0 -->
<dependency>
  <groupId>com.virjar</groupId>
  <artifactId>dungproxy-webmagic7</artifactId>
  <version>0.0.3</version>
</dependency>
```

### webMagic集成
webMagic是国内一个非常优秀的爬虫框架,代理在爬虫中也是经常使用的。所以提供对webMagic的直接支持。方式如下:
```
 public static void main(String[] args) {
      Spider.create(new GithubRepoPageProcessor()).addUrl("https://github.com/code4craft")
              .setDownloader(new DungProxyDownloader()).thread(5).run();
 }
```
其中 GithubRepoPageProcessor是任意的一个page处理器。而我所做的在DungProxyDownloader,也即我重写了下载器。核心代码其实只改了一行(将默认的httpclient换成了自动注册代理池的httpclient),如果你有自己的定制,可以参考我的实现做一下适配即可。

### webMagic手动下线IP

在使用爬虫的时候,经常会遇到IP被封禁,即使我们使用了多个IP切换,也可用有部分IP本身就在目标网站的黑名单中。所以需要对这些IP进行下线处理,但是不同网站的IP封禁表现不一致。如:
- 返回码401、403
- 返回码501,500
- 进入认证页面(这种情况需要考虑是目标网站是否会解封IP。如果会,则应该使用useInterval功能控制IP使用频率,而非下线IP)
- 进入错误页面:如返回百度首页数据等
- 没有任何响应(服务器不返回任何数据,直接不处理请求。表现为HttpNotResponseException)
- 抛出指定异常,类似上一条

DungProxyDownloader支持对这类IP封禁执行IP下线功能的扩展。扩展方法参考sample``com.virjar.dungproxy.client.sample.WebMagicCustomOfflineProxyDownloader``

```
public class WebMagicCustomOfflineProxyDownloader extends DungProxyDownloader {
    @Override
    protected boolean needOfflineProxy(Page page) {
        if( super.needOfflineProxy(page)){//父类默认下线 401和403,你也可以不调用
            return true;
        }else{
            return StringUtils.containsIgnoreCase(page.getRawText(), "包含这个关键字,代表IP被封禁");
        }
    }
    @Override
    protected boolean needOfflineProxy(IOException e) {
        //return e instanceof SSLException;//如果异常类型是SSL,代表IP被封禁,你也可以不实现
        return false;
    }
}
```
[示例代码地址](http://git.oschina.net/virjar/proxyipcenter/tree/master/clientsample/src/main/java/com/virjar/dungproxy/client/samples/webmagic/WebMagicCustomOfflineProxyDownloader.java)

### webMagic兼容0.5.x和0.6.x(在0.0.7版本已废弃此用法)
webMagic最近在实现代理功能,本身代理功能是本项目的核心,所以必然webMagic的代理相关代码变动可能性特别大。目前已经出现了在0.5.3和0.6.0上面的API不兼容问题。
dungProxy对此做了兼容方案,使用DungProxyDownloader可以同时支持0.5.x和0.6.x的用法。也就是说如果您的webMagic版本是0.5.x,那么DungProxyDownloader走0.5.x的代理逻辑,如果你的webMagic版本是0.6.x,那么DungProxyDownloader则会走0.6.x的代理逻辑。两种模式的切换是自动实现的,你不必关心。只需要知道在0.5.x上面怎么使用,然后根据0.5.x的规范进行使用。或者知道0.6.x的功能,然后根据0.6.x的规范使用。

另外注意,本项目本身专注代理IP池的管理。webMagic本身也有代理池的概念。如果使用了webMagic的代理池的同时使用DungProxy,那么DungProxy将不会生效。任何时刻,只有WebMagic定义了,那么已webMagic为主


### webMagic多用户登录
定制的downloader(DungProxyDownloader)默认使用MultiUserCookieStore,天然支持多用户同时在线了。MultiUserCookieStore本身使用分段锁的概念,多个用户在并发上也不会存在锁竞争问题。如何使webMagic支持多用户登录,参考[demo](http://git.oschina.net/virjar/proxyipcenter/tree/master/clientsample/src/main/java/com/virjar/dungproxy/client/samples/webmagic/MultiUserLoginTest.java)
demo看起来有点复杂,其实不复杂,因为他有登录逻辑、登录失效重新登录逻辑、调度器重写等等、证明dungproxy在多用户维护上面没有cookie紊乱。实际上在使用的时候,只需要做到登录之后再添加种子,且将用户绑定再种子上面。之后dungProxy的机制就会自动维护这些URL属于那个用户了(当然登录的时候也需要给httpclient指定当前使用的那个用户,httpclient负责cookie的种植,没有告诉httpclient的话,httpclient也不知道把cookie种植到那个账户下面)。
对了,用户标示只能是字符串。不要放对象,否则出问题别怪我😄

### webMagic注意事项
- 调整timeOut,webMagic的默认超时时间是5秒,这个对于使用代理对的场景来说是不合适的。建议调整到20秒以上
- 对失败有预期,框架只能尽可能减少失败,但是不可能杜绝失败
- 在没有遇到IP被封的时候,没有必要接入本工具
- 参考demo [点击这里](http://git.oschina.net/virjar/proxyipcenter/tree/master/clientsample/src/main/java/com/virjar/dungproxy/client/samples/webmagic/dytt8/WebMagicTest.java)


### 推荐的参数

```
    private Site site = Site.me()
            .setRetryTimes(3) //就我的经验,这个重试一般用处不大
            .setTimeOut(30000)//在使用代理的情况下,这个需要设置,可以考虑调大线程数目
            .setSleepTime(0)//使用代理了之后,代理会通过切换IP来防止反扒。同时,使用代理本身qps降低了,所以这个可以小一些
            .setCycleRetryTimes(3)//这个重试会换IP重试,是setRetryTimes的上一层的重试,不要怕三次重试解决一切问题。。
            .setUseGzip(true);
```