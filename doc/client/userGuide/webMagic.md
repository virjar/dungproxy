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
}
```
[示例代码地址](http://git.oschina.net/virjar/proxyipcenter/tree/master/client/src/test/java/com/virjar/dungproxy/client/sample/WebMagicCustomOfflineProxyDownloader.java)

### webMagic兼容0.5.x和0.6.x
webMagic最近在实现代理功能,本身代理功能是本项目的核心,所以必然webMagic的代理相关代码变动可能性特别大。目前已经出现了在0.5.3和0.6.0上面的API不兼容问题。
dungProxy对此做了兼容方案,使用DungProxyDownloader可以同时支持0.5.x和0.6.x的用法。也就是说如果您的webMagic版本是0.5.x,那么DungProxyDownloader走0.5.x的代理逻辑,如果你的webMagic版本是0.6.x,那么DungProxyDownloader则会走0.6.x的代理逻辑。两种模式的切换是自动实现的,你不必关心。只需要知道在0.5.x上面怎么使用,然后根据0.5.x的规范进行使用。或者知道0.6.x的功能,然后根据0.6.x的规范使用。

另外注意,本项目本身专注代理IP池的管理。webMagic本身也有代理池的概念。如果使用了webMagic的代理池的同时使用DungProxy,那么DungProxy将不会生效。任何时刻,只有WebMagic定义了,那么已webMagic为主


### webMagic注意事项
- 调整timeOut,webMagic的默认超时时间是5秒,这个对于使用代理对的场景来说是不合适的。建议调整到20秒以上
- 对失败有预期,框架只能尽可能减少失败,但是不可能杜绝失败
- 在没有遇到IP被封的时候,没有必要接入本工具
- 参考demo [点击这里](http://git.oschina.net/virjar/proxyipcenter/tree/master/client/src/test/java/com/virjar/dungproxy/client/WebMagicTest.java)
