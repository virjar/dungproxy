## 配置client规则
### 通过参数配置

配置参数是使用配置文件描述一些代理池需要的规则。如确定那些请求需要被代理等。在resources目录添加文件``proxyclient.properties``
![image](../../pic/proxyclient_properties.png)

完整的配置如下:
```
#调研,可配置既可编程
#资源适配器,实现接口com.virjar.dungproxy.client.ippool.strategy.ResourceFacade,可以自定义,然后接入自己的ip源
proxyclient.resouce.resourceFacade=com.virjar.dungproxy.client.ippool.strategy.impl.DefaultResourceFacade
#代理策略配置
#WHITE_LIST,BLACK_LIST,所有请求都要代理,所有请求都不代理,在白名单的请求代理,不在黑名单的请求代理,可以自己实现自己的策略,
proxyclient.proxyDomainStrategy=WHITE_LIST
#配置需要代理的域名,当代理策略为黑名单策略的时候生效
proxyclient.proxyDomainStrategy.backList=115.159.40.202
#配置需要代理的域名,当代理策略为白名单策略的时候生效
proxyclient.proxyDomainStrategy.whiteList=pachong.org,cn-proxy.com,www.sslproxies.org,www.66ip.cn,proxy-list.org,free-proxy-list.net
#两分钟一次向服务器反馈IP使用情况
proxyclient.feedback.duration=120000
#序列化和反序列化接口,可以通过他导出自己需要的数据到想要的地方,可以将ip快照保持在某个地方,如放到文件,数据库等等
proxyclient.serialize.serializer=com.virjar.dungproxy.client.ippool.strategy.impl.JSONFileAvProxyDumper
#和JSONFileAvProxyDumper配合,将会把这个配置传递到序列化器。如果自定义实现不需要这个配置,那么也可以不配置他
proxyclient.DefaultAvProxyDumper.dumpFileName=/Users/virjar/git/proxyipcenter/client/product/availableProxy.json
#server统一代理服务,在本地IP池IP不足量的时候,尝试使用统一的中心服务器,他会进行转发代理。目前本模块没有经过充足测试,可以不用配置
proxyclient.defaultProxyList=115.159.40.202:8081
#对于预热器的一个配置,规定预热器检查的URL列表,逗号分割(如果有多个URL需要检测)
proxyclient.preHeater.testList=https://www.douban.com/group/explore
#默认中心服务器地址,如果IP下载器是默认实现(com.virjar.dungproxy.client.ippool.strategy.impl.DefaultResourceFacade),那么他会使用这个地址。否则本配置无效
proxyclient.resource.defaultResourceServerAddress=http://proxy.scumall.com:8080
#预热器增量序列化,在测试通过一定数目的资源的时候,就会将数据序列化,防止长时间运行,任务中断预热数据丢失
proxyclient.preHeater.serialize.step=30
#IP使用间隔,单位是毫秒,有些场景,IP有明确的控制说QPS,可以考虑使用这个参数来控制。如一分钟访问不能超过20次,那么 (60 * 1000 / 20) ,可以配置proxyclient.proxyUseIntervalMillis=3000
proxyclient.proxyUseIntervalMillis=0
#客户端ID,配置此参数拥有相同ID配置的所有节点的IP资源不重复
proxyclient.clientID=com.virjar.webcrawler
#代理规则路由,多个域名可以使用同一个代理规则,domain:similarDomain1,similarDomain2;domain2:similarDomain3,similarDomain4...
proxyclient.proxyDomainStrategy.group=www.dytt8.net:www.ygdy8.net
```


### 通过代码定制策略

#### 定制配置规则
DungProxy在最简化使用模式下,默认走配置文件,但是为了提供更加良好的扩展性。DungProxy还支持通过代码的方式定制策略。同时,当前配置文件的方式在DungProxy内部也是转化为对应代码API调用。所以代码调用将会比配置文件的方式灵活,部分配置只支持了代码的方式。DungProxy通过代码定制策略的入口如下:
1. 获取DungProxyContext
   DungProxyContext是IpPool的一个上下文,默认情况IpPool是一个单例的,但是你也可以通过多个DungProxyContext产生多个IpPool实例,虽然我觉得这样意义不大。因为DungProxy本身就设计为支持多域名多规则的线程安全工具。
   调用``com.virjar.dungproxy.client.ippool.config.DungProxyContext.create``就可以获取一个DungProxyContext的对象了,此时的Context是一个填入了默认策略的上下文对象,您可以对他进行进行进行定制,可定制项包括了IP池全局策略和某个域名的策略。具体可以定制的内容请看到API或者参考配置文件说明。
2. 获取DomainContext
   DomainContext是针对于某个域名的差异化配置,如可以配置不同域名下IP池的容量大小,确定每个IP的最小使用时间间隔等。当你没有定义这些规则的时候,将会继承获取DungProxyContext的对应配置,如果获取DungProxyContext也没有定义,则走获取DungProxyContext的默认规则。
   DomainContext不允许直接创建,需要由DungProxyContext产生,因为他必须建立和DungProxyContext的联系,链接逻辑在DungProxyContext的创建方法中``com.virjar.dungproxy.client.ippool.config.DungProxyContext.genDomainContext``
   同样的,拿到DomainContext之后就可以对他进行定制了,定制完成后不需要保存对象,因为他已经存在DungProxyContext里面了
   
####由DungProxyContext产生IPPool对象
调用方法``com.virjar.dungproxy.client.ippool.IpPool.create`` 传递DungProxyContext到create方法即可

####构造默认IpPool
IpPool存在一个默认的实例,并且是单例静态的,因为作者希望IP池的使用足够简单,甚至可以简单到当成静态方法调用。IpPool的默认实例存放在``com.virjar.dungproxy.client.ippool.IpPoolHolder``,他是一个懒汉式,在调用``com.virjar.dungproxy.client.ippool.IpPoolHolder.getIpPool``的时候将会检查默认实例是否加载,并且尝试加载。所以这个时候如果初始化IP池,将会根据默认规则读取配置文件。
为了支持将包含自定义规则的上下文注入默认IPPool,IpPoolHolder提供了另一个静态方法``com.virjar.dungproxy.client.ippool.IpPoolHolder.init``,传递DungProxyContext,则会使用传入的上下文初始化默认IPPool,但是需要注意执行本逻辑之前,没有直接或者间接的调用getIpPool方法,否则会导致默认规则加载。

#### 例如云代理的配置
```
//云代理对象
AvProxyVO avProxyVO = new AvProxyVO();
avProxyVO.setIp("proxy.abuyun.com");//阿布云服务器地址
avProxyVO.setPort(9010);//阿布云服务器端口
avProxyVO.setCloud(true);//标示这个代理是云代理
avProxyVO.setCloudCopyNumber(4);//个人阿布云账户是支持4个并发,使用者填写各自的并发数目
//如果是通过头部认证的方式,则如下添加
//avProxyVO.setAuthenticationHeaders(HeaderBuilder.create().withHeader("Proxy-Authorization", "token").buildList());
avProxyVO.setUsername("H402MPRHB15K37YD");//代理帐户
avProxyVO.setPassword("601AE248E2ABE744");//代理密码


// Step2 创建并定制代理规则 DungProxyContext
DungProxyContext dungProxyContext = DungProxyContext.create().addCloudProxy(avProxyVO); // Step3
// Step 3 使用代理规则构造默认IP池
IpPoolHolder.init(dungProxyContext);

```

代码配置策略入口有很多,他是配置文件入口的超集,具体可配置项可以自己看看DungProxyContext和获取DomainContext的API