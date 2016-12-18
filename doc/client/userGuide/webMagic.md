### webMagic集成
webMagic是国内一个非常优秀的爬虫框架,代理在爬虫中也是经常使用的。所以提供对webMagic的直接支持。方式如下:
```
 public static void main(String[] args) {
      Spider.create(new GithubRepoPageProcessor()).addUrl("https://github.com/code4craft")
              .setDownloader(new DungProxyDownloader()).thread(5).run();
 }
```
其中 GithubRepoPageProcessor是任意的一个page处理器。而我所做的在DungProxyDownloader,也即我重写了下载器。核心代码其实只改了一行(将默认的httpclient换成了自动注册代理池的httpclient),如果你有自己的定制,可以参考我的实现做一下适配即可。
