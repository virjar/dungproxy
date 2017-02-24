#统一代理服务

定义为统一代理服务,就是在服务器统一转发客户端的请求,感觉这个和云没有关系。但是现在云代理貌似挺火的啊,不知道我这个统一代理和云代理会不会是类似的

设计统一代理服务的原因是对于服务器IP资源来说,至少证明服务器到代理服务器的线路是通的。他要比客户端到代理服务器的可用性概率大一些。同时服务器可以控制走私有线路(后续功能)用来保证高可用。
目前统一代理服务不是强制使用,按照默认规则来说,当本地IP池IP还没有成功加载的时候,将会使用统一代理服务。要是统一代理服务生效,需要配置服务器地址:


dungProxy-server本身支持云代理转发功能,他是使用netty实现的高性能代理模块,但是受限于本项目经费问题,云代理一直没有启用。但是dungProxy本身就支持和各类云代理混合调用的方案。可以同时支持各类IP来源、各类云代理供应商、免费的收费的同时在dungpxoy里面使用,dungPxoy基于smartProxyQueue代理队列模型对各种资源进行调度,打分,顺序调整等功能,尽量保证优质资源被使用。

在sample里面有一个阿布云&dungProxy&WebMagic[联合使用的案例](http://git.oschina.net/virjar/proxyipcenter/tree/master/clientsample/src/main/java/com/virjar/dungproxy/client/samples/webmagic/WebMagicCloudProxy.java),参见他实现混合接入