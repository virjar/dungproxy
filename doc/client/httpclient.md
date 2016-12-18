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