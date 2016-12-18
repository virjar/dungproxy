#IpPool的扩展点,自定义一些策略

### 扩展点列表
- 定义数据来源(接入你自己的代理)
- 定义代理规则(那些请求需要被代理)
- 定义可用IP序列化规则(可以将可用IP实时保存到你想要保存地方)
- 定义下线规则
- 定义打分策略

com.virjar.dungproxy.client.ippool.strategy.ResourceFacade:实现此类,接入数据源(可以考虑同时接入dungProxy—server和自己的IP)
com.virjar.dungproxy.client.ippool.strategy.ProxyDomainStrategy:传递一个域名,判断该域名下的请求是否需要被代理
com.virjar.dungproxy.client.ippool.strategy.AvProxyDumper 确定如何实时序列化可用IP
com.virjar.dungproxy.client.ippool.strategy.Offline 确定一个IP什么时候会被下线
com.virjar.dungproxy.client.ippool.strategy.Scoring 确定如何为IP打分


### 通过配置文件设置扩展
默认情况下,IpPool通过配置文件加载各种规则,且通过反射的方式加载扩展规则。配置文件需要位于classPath,文件名称为:``proxyclient.properties`
接口和对应配置key的对应关系如下:
|key|接口类|默认值|
|--|--|--|
|proxyclient.resouce.resourceFacade|com.virjar.dungproxy.client.ippool.strategy.ResourceFacade|com.virjar.dungproxy.client.ippool.strategy.impl.DefaultResourceFacade|
|proxyclient.proxyDomainStrategy|com.virjar.dungproxy.client.ippool.strategy.ProxyDomainStrategy|com.virjar.dungproxy.client.ippool.strategy.impl.WhiteListProxyStrategy|
|proxyclient.serialize.serializer|com.virjar.dungproxy.client.ippool.strategy.AvProxyDumper|com.virjar.dungproxy.client.ippool.strategy.impl.JSONFileAvProxyDumper|
|暂不支持配置|com.virjar.dungproxy.client.ippool.strategy.Offline|com.virjar.dungproxy.client.ippool.strategy.impl.DefaultOffliner|
|暂不支持配置|com.virjar.dungproxy.client.ippool.strategy.Scoring|com.virjar.dungproxy.client.ippool.strategy.impl.DefaultScoring|


### 通过代码设置扩展

正常情况下,调用IpPool实例的时候,就会进行各种规则加载初始化动作,这个时候就会去找默认配置文件了,所以需要在自动初始化动作之前调用手动调用初始化动作
方法路径是:``com.virjar.dungproxy.client.ippool.config.Context.initEnv``,他是一个静态方法:
如:
```
        Context.ConfigBuilder configBuilder = Context.ConfigBuilder.create()
                .setAvDumper("我的序列化实现")
                .setResouceFace("我的资源引入实现");
        Context.initEnv(configBuilder);
```