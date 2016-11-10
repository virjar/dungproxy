# DrungProxy
DrungProxy是一个代理IP服务,他包括一个代理IP资源server端和一系列适配中心IP资源得客户端。server负责代理IP资源得收集维护。client则是一系列方便用户使用得API,他屏蔽了代理IP下载,代理IP选取,IP绑定得比较复杂逻辑。用户只需要引入client即可方便使用代理IP服务

## [server相关说明](./doc/server.md )

## [java client相关说明](./doc/client.md)
- client配置
- IpPool IP池的核心
- 和HttpClient集成
- HttpClient封装
- webMagic集成
- 和webCollector的集成
- preHeater 资源预热
- 统一代理服务(和server端配合)
- 其他支持

### [client quick start](./doc/client_quick_start.md)
- 获取代码
- 添加依赖
- 配置参数
- 启动预热器
- 使用代理访问
- 使用webMagic


qq group 
> 569543649

## 案例
[代理IP爬虫](http://115.159.40.202:8080/#/index) 我们通过代理访问封堵我们的代理资源发布网站,以及访问国外的代理IP网站

### serverList
115.159.40.202 收集&分发
115.28.177.116 验证
114.215.144.211 地址同步&数据备份&域名下线&域名刷新

## 贡献者
- 邓维佳 virjar@virjar.com
- 符灵通 
- 韦轩 805366180@qq.com