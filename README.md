# DungProxy
DungProxy是一个代理IP服务,他包括一个代理IP资源server端和一系列适配中心IP资源得客户端。server负责代理IP资源的收集维护。client则是一系列方便用户使用得API,他屏蔽了代理IP下载、代理IP选取、IP绑定、IP切换等比较复杂逻辑。用户只需要引入client即可方便使用代理IP服务


## [client使用手册](doc/client/userGuide/README.md)

## [演示视频](https://pan.baidu.com/s/1hrZnINq)

## [java client设计说明](doc/client/design/README.md)

## [server 部署手册](doc/server/deploy/README.md)

## [server设计说明](doc/server/deploy/README.md )

请注意,在没有遇到封IP的情况下,不要尝试使用本工具。使用代理的效果肯定比不上不使用代理

qq 群,人肉文档支持😁 
> 569543649

## 案例
[代理IP爬虫](http://114.215.144.211:8080/#/index) 我们通过代理访问封堵我们的代理资源发布网站,以及访问国外的代理IP网站

##快速开始
```
      // Step1 代理策略,确定那些请求将会被代理池代理
      WhiteListProxyStrategy whiteListProxyStrategy = new WhiteListProxyStrategy();
      whiteListProxyStrategy.addAllHost("www.baidu.com");

      // Step2 创建并定制代理规则
      DungProxyContext dungProxyContext = DungProxyContext.create().setNeedProxyStrategy(whiteListProxyStrategy);
     
      // Step3 使用代理规则初始化默认IP池
      IpPoolHolder.init(dungProxyContext);

      // Step4 使用CrawlerHttpClient或者任何基于Httpclient插件植入IP池的方式调用IP池的API
      HttpInvoker.get("http://www.baidu.com");
```

### serverList
115.159.40.202 收集&分发
123.56.155.209 验证
114.215.144.211 地址同步&数据备份&域名下线&域名刷新

## 贡献者
- 邓维佳 virjar@virjar.com
- 符灵通 
- 韦轩 805366180@qq.com