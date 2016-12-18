client其实是一个独立的项目,他专注于本地代理IP池的管理。他有一些默认策略,其中默认IP数据来源策略和我们的server适配。他是一个精简API,尽量不依赖与其他框架,使得它可以在跟多的环境下使用。

## [构建客户端代码](build_code.md)

## [预热IP资源](warm.md) 如果你的IP是私有高质量IP,可以考虑不进行此步骤

## [集成client到你的工程](integration.md)

## [调用IP池对象](ip_pool.md) (可选)

## [集成IP池到httpclient](httpclient.md) (可选)

## [使用已经集成好代理池的httpclient(CrawlerHttpClient)](crawler_httpclient.md)(可选)

## [使用webMagic](webMagic.md)(适合WebMagic爬虫用户)

## [使用webCollector](webCollector.md)(适合webCollector爬虫用户)

## [使用统一代理服务(云代理)](cloud_proxy.md)

## [配置扩展点](extension.md)
