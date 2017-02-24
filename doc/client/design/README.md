#client运行原理
这里讲述IP池的设计相关,如果您仅仅是为了使用dunproxy-client,则不必关心本文内容

client就是一个代理IP池的实现,IP池的设计基于两个点 1)代理IP都是不稳定的,不可靠的,需要一个机制来切换IP,尽可能使用高质量IP。2)IP和环境关系很大,同一个IP在不同的机器下访问不同的目标网站,其可用性表现都是不一样的

##[client系统结构](architecture.md)

##[smartProxyQueue顺序惩罚容器](SmartProxyQueue.md)

##[权值稀释的分数叠加算法](scoring.md)

##[webMagic->httpClient->IpPool的调用流程](DungProxyDownloader.md)