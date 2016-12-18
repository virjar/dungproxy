#在httpclient里面使用IP池

##集成IpPool到原生HttpClient
在dungpoxy-client里面,实现了对httpclient的适配,这样可以很方便的集成到httpclient里面,因为适配器实现在httpclient下层,所以即使你有自己的httpclient封装,也不妨碍在你自己封装的httpclient里面使用dungproxy。

我们实现了两个httpclient扩展点
- ``com.virjar.dungproxy.client.httpclient.conn.ProxyBindRoutPlanner``,负责再合适的时候绑定分配和绑定IP
- ``com.virjar.dungproxy.client.httpclient.DunProxyHttpRequestRetryHandler``,统计拦截IP使用失败的情况,以及触发IP下线
在创建httpclient实例的时候,织入两个扩展点到你的httpclient即可

使用案例如下:
```
CloseableHttpClient closeableHttpClient =
         HttpClientBuilder.create().setRetryHandler(new DunProxyHttpRequestRetryHandler())
                  .setRoutePlanner(new ProxyBindRoutPlanner()).build();
```
其中DunProxyHttpRequestRetryHandler是一个代理模式,因为他的主要功能不是定制重试策略,而是根据失败信息统计代理IP下线逻辑。所以你可以在构造的时候传入你自己的handler,这样你的策略可以和IP自动下线功能共存。
对于ProxyBindRoutPlanner则不能自己定制了,如果要扩展,需要继承他甚至重写他,不过逻辑不复杂。

DunProxyHttpRequestRetryHandler和自定义handler共存方法
```
DunProxyHttpRequestRetryHandler dungHandler = new DunProxyHttpRequestRetryHandler(YourHandler);
```

##在httpclient上面操作代理IP
对于IPPool,httpclient属于IpPool上层,有些场景需要提供上层控制IpPool某个表现的能力。httpClient绑定代理IP之后,将会把IP注册到httpclientContext之中,这样就可以通过httpclientContext获取IP实例了
有一个工具类用于操作IP ``com.virjar.dungproxy.client.util.PoolUtil1``提供的接口如下:
- com.virjar.dungproxy.client.util.PoolUtil.recordFailed 记录IP使用失败
- com.virjar.dungproxy.client.util.PoolUtil.offline 下线当前绑定到httpClientContext上面的IP
- com.virjar.dungproxy.client.util.PoolUtil.cleanProxy 删除绑定在当前httpClientContext上面的IP(这样会强制切换IP),和下线不一样的是,他不会导致IP被IP池移除
- com.virjar.dungproxy.client.util.PoolUtil.bindUserKey 注册当前模拟账户,这样不管代理池状态如何,都会绑定同一个IP(如果IP没有下线)
- com.virjar.dungproxy.client.util.PoolUtil.getBindProxy 获取绑定在httpClientContext的IP



