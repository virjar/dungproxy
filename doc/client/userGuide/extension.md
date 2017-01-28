#IpPool的扩展点,自定义一些策略

### 扩展点列表
- 定义数据来源(接入你自己的代理)
- 定义代理规则(那些请求需要被代理)
- 定义可用IP序列化规则(可以将可用IP实时保存到你想要保存地方)
- 定义下线规则
- 定义打分策略

com.virjar.dungproxy.client.ippool.strategy.ResourceFacade:实现此类,接入数据源(可以考虑同时接入dungProxy—server和自己的IP)
 本扩展点有两个demo实现,分别是[CustomIPSource](http://git.oschina.net/virjar/proxyipcenter/tree/master/clientsample/src/main/java/com/virjar/dungproxy/client/samples/poolstrategy/CustomIPSource.java)和[CombineIpSource](http://git.oschina.net/virjar/proxyipcenter/tree/master/clientsample/src/main/java/com/virjar/dungproxy/client/samples/poolstrategy/CombineIpSource.java) 分别实现导入IP文件和实现多数据源同时导入
com.virjar.dungproxy.client.ippool.strategy.ProxyDomainStrategy:传递一个域名,判断该域名下的请求是否需要被代理
com.virjar.dungproxy.client.ippool.strategy.AvProxyDumper 确定如何实时序列化可用IP
com.virjar.dungproxy.client.ippool.strategy.Offline 确定一个IP什么时候会被下线
com.virjar.dungproxy.client.ippool.strategy.Scoring 确定如何为IP打分


### 通过配置文件设置扩展
默认情况下,IpPool通过配置文件加载各种规则,且通过反射的方式加载扩展规则。配置文件需要位于classPath,文件名称为:``proxyclient.properties``
接口和对应配置key的对应关系如下:

|key|接口类|默认值|
|----|----|----|
|proxyclient.resouce.resourceFacade|com.virjar.dungproxy.client.ippool.strategy.ResourceFacade|com.virjar.dungproxy.client.ippool.strategy.impl.DefaultResourceFacade|
|proxyclient.proxyDomainStrategy|com.virjar.dungproxy.client.ippool.strategy.ProxyDomainStrategy|com.virjar.dungproxy.client.ippool.strategy.impl.WhiteListProxyStrategy|
|proxyclient.serialize.serializer|com.virjar.dungproxy.client.ippool.strategy.AvProxyDumper|com.virjar.dungproxy.client.ippool.strategy.impl.JSONFileAvProxyDumper|
|暂不支持配置|com.virjar.dungproxy.client.ippool.strategy.Offline|com.virjar.dungproxy.client.ippool.strategy.impl.DefaultOffliner|
|暂不支持配置|com.virjar.dungproxy.client.ippool.strategy.Scoring|com.virjar.dungproxy.client.ippool.strategy.impl.DefaultScoring|


### 通过代码设置扩展

##DungProxy在最简化使用模式下,默认走配置文件,但是为了提供更加良好的扩展性。DungProxy还支持通过代码的方式定制策略。同时,当前配置文件的方式在DungProxy内部也是转化为对应代码API调用。所以代码调用将会比配置文件的方式灵活,部分配置只支持了代码的方式。DungProxy通过代码定制策略的入口如下:
1. 获取DungProxyContext
   DungProxyContext是IpPool的一个上下文,默认情况IpPool是一个单例的,但是你也可以通过多个DungProxyContext产生多个IpPool实例,虽然我觉得这样意义不大。因为DungProxy本身就设计为支持多域名多规则的线程安全工具。
   调用``com.virjar.dungproxy.client.ippool.config.DungProxyContext.create``就可以获取一个DungProxyContext的对象了,此时的Context是一个填入了默认策略的上下文对象,您可以对他进行进行进行定制,可定制项包括了IP池全局策略和某个域名的策略。具体可以定制的内容请看到API或者参考配置文件说明。
2. 获取DomainContext
   DomainContext是针对于某个域名的差异化配置,如可以配置不同域名下IP池的容量大小,确定每个IP的最小使用时间间隔等。当你没有定义这些规则的时候,将会继承获取DungProxyContext的对应配置,如果获取DungProxyContext也没有定义,则走获取DungProxyContext的默认规则。
   DomainContext不允许直接创建,需要由DungProxyContext产生,因为他必须建立和DungProxyContext的联系,链接逻辑在DungProxyContext的创建方法中``com.virjar.dungproxy.client.ippool.config.DungProxyContext.genDomainContext``
   同样的,拿到DomainContext之后就可以对他进行定制了,定制完成后不需要保存对象,因为他已经存在DungProxyContext里面了
   
##由DungProxyContext产生IPPool对象
调用方法``com.virjar.dungproxy.client.ippool.IpPool.create`` 传递DungProxyContext到create方法即可

##构造默认IpPool
IpPool存在一个默认的实例,并且是单例静态的,因为作者希望IP池的使用足够简单,甚至可以简单到当成静态方法调用。IpPool的默认实例存放在``com.virjar.dungproxy.client.ippool.IpPoolHolder``,他是一个懒汉式,在调用``com.virjar.dungproxy.client.ippool.IpPoolHolder.getIpPool``的时候将会检查默认实例是否加载,并且尝试加载。所以这个时候如果初始化IP池,将会根据默认规则读取配置文件。
为了支持将包含自定义规则的上下文注入默认IPPool,IpPoolHolder提供了另一个静态方法``com.virjar.dungproxy.client.ippool.IpPoolHolder.init``,传递DungProxyContext,则会使用传入的上下文初始化默认IPPool,但是需要注意执行本逻辑之前,没有直接或者间接的调用getIpPool方法,否则会导致默认规则加载。

