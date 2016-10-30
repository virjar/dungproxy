client其实是一个独立的项目,他专注于本地代理IP池的管理。他有一些默认策略,其中默认IP数据来源策略和我们的server适配。他是一个精简API,尽量不依赖与其他框架,使得它可以在跟多的环境下使用。

## client依赖:
- guava,google的工具类
- logback,client默认使用logback打印日志
- common-lang3 common-io 也是工具类
- httpclient
- 没有啦,其他的感觉都不是必要的,即使现在有,等一段时间也会把它精简出去

## client配置
client使用其实是需要配置的,我们希望不用做任何配置就可以实现代理功能,但是这样会大大导致灵活性。所以有一点点必要的配置也是可以的。比如我们总应该配置哪些请求需要走代理,哪些不需要吧。
默认client配置名称是``proxyclient.properties``这个文件名称是确定的,在proxyConstant中记录。事例如下
```
proxyclient.resouce.resourceFacade=com.virjar.dungproxy.client.ippool.strategy.resource.DefaultResourceFacade
#代理策略配置
#WHITE_LIST,BLACK_LIST,所有请求都要代理,所有请求都不代理,在白名单的请求代理,不在黑名单的请求代理
proxyclient.proxyDomainStrategy=WHITE_LIST
#配置需要代理的域名,当代理策略为黑名单策略的时候生效
proxyclient.proxyDomainStrategy.backList=115.159.40.202
#配置需要代理的域名,当代理策略为白名单策略的时候生效
proxyclient.proxyDomainStrategy.whiteList=pachong.org,cn-proxy.com,www.sslproxies.org,www.66ip.cn,proxy-list.org,free-proxy-list.net
#两分钟一次向服务器反馈IP使用情况
proxyclient.feedback.duration=120000
proxyclient.serialize.serializer=com.virjar.dungproxy.client.ippool.strategy.serialization.JSONFileAvProxyDumper
prxyclient.DefaultAvProxyDumper.dumpFileName=/tmp/proxyclient/availableProxy.json
#server统一代理服务
proxyclient.defaultProxy=115.159.40.202:8081
proxyclient.preHeater.testList=http://www.66ip.cn/3.html
```
最简配置还可以是这样
```
proxyclient.proxyDomainStrategy.whiteList=pachong.org,cn-proxy.com,www.sslproxies.org,www.66ip.cn,proxy.goubanjia.com,proxy-list.org,free-proxy-list.net
```
当前你什么都不配也可以使用,只是他不会代理任何请求,只是当作普通的httpclient使用了


## client三个组成模块
client定位与代理IP池,所以主要也是干这个的。但是考虑到我们自己要决定什么时候使用代理,什么时候销毁他,还有和我们自己的网络访问层集成。所以我们有做了一个模块,就是把这个代理IP池集成到httpclient(需要的httpclient版本是4.4+,选择这个版本是拍脑门儿选择的,没有原因,如果有问题可以尝试降低版本)里面。再然后,发现使用httpclient请求一次数据实在麻烦,按照httpclient原生方式发送一个get请求不知道要写多少代码,所以顺便又在httpclient上面做了一个封装,提供了常见场景下的请求包装。
### IpPool IP池的核心
IpPool是单例的,做成单例的原因是,没有地方存放这个实例。我希望多个httpclient能够共用同一份IpPool,但是有不想侵入到httpclient里面。所以就单例了。想想貌似也不会太恶心吧。
#### IpPool 数据模型
分为两层,算起来就是一个multiMap结构,外层是一个总容器,根据domain放置了各自的domainPool,domainPool里面是一个IP资源集合,存放这个domain下面的可用IP。domainPool里面的IP不是list,是一个TreeMap,通过他实现了一个一致性hash结构,使IP绑定支持一致性hash的方式。
#### 资源引入接口 ``com.virjar.ipproxy.ippool.strategy.resource.ResourceFacade``
可以自己定义IP导入方式,系统一个默认实现``com.virjar.dungproxy.client.ippool.strategy.resource.DefaultResourceFacade``,默认通过DungProxy的server请求数据。这个处理器要完成两个任务,下载IP资源,对IP资源进行使用反馈。
#### 判定哪些请求需要被代理 ``com.virjar.ipproxy.ippool.strategy.proxydomain.ProxyDomainStrategy``
默认有两个实现,黑名单方式和白名单方式。默认使白名单方式,只有再白名单中的host才会被代理,相反黑名单方式除了黑名单之中的host都会被代理(有意思的是如果你代理IP资源引入请求,会有死递归的问题)
#### 序列化和反序列化 ``com.virjar.ipproxy.ippool.strategy.serialization.AvProxyDumper``
本地IP池的资源是可贵的,对于某些网站可能需要运行很久才能使得这个IP池资源比较有效。因为本地IP池会随时刷新资源,下线资源。只有有效资源才能再一次一次的下线事件中保留下来。所以他是最契合本地环境的一批资源,特别是系统运行时间越长。所以我们会通过这个接口定时的将IP池dump出来,然后序列化。再下次系统启动的时候,优先反序列化这个数据。本来我是没有设置这个功能的,主要是我在本地调试的时候频繁起停程序,每次都需要很长时间才能够使得IPPool有效(有些网站特炒蛋)
#### 判定资源何时下线 ``com.virjar.ipproxy.ippool.strategy.offline.Offline``
如果一个IP反馈说失败了,那么将会这个接口判定是否需要下线这个IP。当然你也可以获取到代理IP的实例,手动控制下线
#### 规则加载
我们也遵循可配置即可编程的原则,虽然我们默认提供了一个配置文件的方式控制各种策略,但是你完全可以不来这一套。可以通过``com.virjar.dungproxy.client.ippool.config.Context.ConfigBuilder``来实现代码定制规则。当前我们系统也是通过他来实现properties文件的规则加载的。获取Builder实例之后,可以通过代码对他定制,然后使用``com.virjar.ipproxy.ippool.config.Context.initEnv``注册配置。不好意思,这个Context也是静态的,就当我再一次拍脑门儿了吧。
#### IP获取
获取一个IP的方式是这样的 ``IpPool.getInstance().bind(target.getHostName(), accessUrl, user);`` 第一个参数是域名,第二个参数是你当前需要访问的url,第三个参数是当前那个用户再访问。第二个参数和第三个参数可以忽略,第一个不能,因为我们本来根据域名来维护IP池的。第二个参数的作用是:(如果IP不够,将会把这个url发送到服务器,服务器会根据这个url离线跑出可用IP,放置到对应的域名IP池)。第三个参数代表用户,这个用户是逻辑上的,主要场景是存在多个账户登录同一个网站进行数据抓取,这个时候希望每个用户再每次请求的时候,尽量(通过一致性hash尽量包装)获取的是同一个IP。当然传入null代表随机绑定IP了。
#### IP下线
IP下线很简单,拿到IP实例,这样调用``com.virjar.dungproxy.client.model.AvProxy.offline()``。当然其实我不建议这么做,最好的方式是记录一次失败使用``com.virjar.dungproxy.client.model.AvProxy.recordFailed()``。然后他会尝试问一问下线策略是否需要下线IP

### 和HttpClient集成
和HttpClient集成很简单,只需要再HttpclientBuilder的时候,植入两个类即可
- ``com.virjar.dungproxy.client.httpclient.conn.ProxyBindRoutPlanner``,负责再合适的时候绑定分配和绑定IP
- ``com.virjar.dungproxy.client.httpclient.DunProxyHttpRequestRetryHandler``,统计拦截IP使用失败的情况,以及触发IP下线
使用案例如下:
```
CloseableHttpClient closeableHttpClient =
         HttpClientBuilder.create().setRetryHandler(new DunProxyHttpRequestRetryHandler())
                  .setRoutePlanner(new ProxyBindRoutPlanner()).build();
```
其中DunProxyHttpRequestRetryHandler是一个代理模式,因为他的主要功能不是定制重试策略,而是根据失败信息统计代理IP下线逻辑。所以你可以在构造的时候传入你自己的handler,这样你的策略可以和IP自动下线功能共存。
对于ProxyBindRoutPlanner则不能自己定制了,如果要扩展,需要继承他甚至重写他,不过逻辑不复杂。

### HttpClient封装
封装Httpclient的原因是,我觉得Build Httpclient都是一个麻烦事儿,所以提供了一个HttpClientBuilder,希望再build的时候,自动把两个组件植入进去。然而发现InnerHttpClient不让我在外面创建,怒了之后把InnerHttpclient的代码给扒出来了。然后我发现可以做的事情很多了,比如默认的User-agent给替换成一个真实浏览器,而不是HttpClient,比如封装常见的访问接口。一可以直接传入一个url拿到数据等等。
- CrawlerHttpClientBuilder 我们的httpclient构建器,他会默认注册代理IP池所提供的适配组件,提供自动代理功能。
- CrawlerHttpClient 修改自InnerHttpClient,我在上面做了两件事情,提供常见场景的http请求接口封装,请求条件有多种,返回都是字符串。第二件事是提供了一个字符集探测的功能,这个功能很有必要,我们是一个浏览器身份的客户端,面对的是各种无良网站服务器,他们的字符集种类太多,我们不可能每次都能本地指定目标网站的字符集,特别是有些时候逻辑上都不知道我们访问的是那个网站。
- HttpInvoker 以静态的方式代理CrawlerHttpClient,并实现场景场景的网络请求封装。这样会维护一个Httpclient实例,所有请求都会使用同一个httpclient。所以需要注意的是,他不适合多个模拟用户同时访问一个网站,因为所有请求都共用同一个cookie空间。当然为每个用户维护一个CrawlerHttpClient是可以实现多个用户同时在线的。

### webMagic集成
webMagic是国内一个非常优秀的爬虫框架,代理在爬虫中也是经常使用的。所以提供对webMagic的直接支持。方式如下:
```
 public static void main(String[] args) {
      Spider.create(new GithubRepoPageProcessor()).addUrl("https://github.com/code4craft")
              .setDownloader(new DungProxyDownloader()).thread(5).run();
 }
```
其中 GithubRepoPageProcessor是任意的一个page处理器。而我所做的在DungProxyDownloader,也即我重写了下载器。核心代码其实只改了一行(将默认的httpclient换成了自动注册代理池的httpclient),如果你有自己的定制,可以参考我的实现做一下适配即可。

### 和webCollector的继承
webCollector是国内另一个比较流行的java爬虫框架,我也对他提供直接支持。方式如下:
继承``com.virjar.dungproxy.client.webcollector.DungProxyAutoParserCrawler``
```
import com.virjar.dungproxy.client.webcollector.DungProxyAutoParserCrawler;

import cn.edu.hfut.dmic.webcollector.model.CrawlDatums;
import cn.edu.hfut.dmic.webcollector.model.Page;

/**
 * Created by virjar on 16/10/31.
 */
public class WebColletorTest extends DungProxyAutoParserCrawler {
    public WebColletorTest(boolean autoParse) {
        super(autoParse);
        this.addSeed("http://www.66ip.cn/2.html");
    }

    public static void main(String[] args) throws Exception {
        new WebColletorTest(true).start(10);
    }

    @Override
    public void visit(Page page, CrawlDatums next) {
        // 页面处理逻辑
    }
}
```
### preHeater 预热器
所有IP在真正上线前都必须通过预热器,预热器的作用是在代理IP真正服务之前,检查是否真正可以使用。预热分为在线预热和离线预热两种。
- 在线预热是指IP资源在运行时不足,有资源导入器自动导入了IP,这个时候预热器检查他是否有效,然后决定是否真正的加入资源池。他是一个异步动作,预热逻辑在单独线程完成,这个时候同步的IP获取逻辑如果找不到合适的IP,则会加载默认IP,也即统一代理服务
- 离线预热则是在爬虫任务启动前,执行preHeat逻辑,他会反序列化当前存在的资源,同事全量刷服务器可用IP数据。完成后会将数据序列化出去,真正运行业务逻辑的时候,将会使用这个成果。建议走离线preHeat
离线预热的方式是:
1. 配置需要检查的URLList ``proxyclient.preHeater.testList=http://www.66ip.cn/3.html``多个的话,逗号分割。
2. 启动preHeater主函数``com.virjar.dungproxy.client.ippool.PreHeater.main``
3. 另一种启动方式:client/product里面存在一个client的发布版本,执行jar文件即可``java -jar client-0.0.1-SNAPSHOT.jar``
```
bogon:proxyclient virjar$ cd ~/git/proxyipcenter/
bogon:proxyipcenter virjar$ ls
README.md			ddl_version1.1.sql		pom.xml				repeater
catalina.base_IS_UNDEFINED	doc				proxyipcenter-dotnet		server
client				hs_err_pid1394.log		proxyipcenter.iml
bogon:proxyipcenter virjar$ cd client/product/
bogon:product virjar$ ls
catalina.base_IS_UNDEFINED	client-0.0.1-SNAPSHOT.jar	lib				logback.xml			proxyclient.properties
bogon:product virjar$ java -jar client-0.0.1-SNAPSHOT.jar 
2016/10/31-01:37:41 INFO  [main] c.v.d.c.u.IpAvValidator:38>>local IP:192.168.0.102
2016/10/31-01:37:41 WARN  [main] c.v.d.c.i.c.Context$ConfigBuilder:291>>不能链接到默认代理服务器,默认代理资源加载失败
2016/10/31-01:37:45 WARN  [pool-1-thread-25] c.v.d.c.i.DomainPool:182>>IP offline {"avgScore":0,"disable":true,"domainPool":{"domain":"pachong.org","resourceFacade":{}},"failedCount":0,"init":true,"ip":"218.205.76.131","port":8080,"referCount":0}
2016/10/31-01:37:45 WARN  [pool-1-thread-13] c.v.d.c.i.DomainPool:182>>IP offline {"avgScore":0,"disable":true,"domainPool":{"domain":"pachong.org","resourceFacade":{}},"failedCount":0,"init":true,"ip":"111.1.23.162","port":80,"referCount":0}
2016/10/31-01:37:45 WARN  [pool-1-thread-27] c.v.d.c.i.DomainPool:182>>IP offline {"avgScore":0,"disable":true,"domainPool":{"domain":"proxy.goubanjia.com","resourceFacade":{}},"failedCount":0,"init":true,"ip":"218.205.76.131","port":8080,"referCount":0}
```

### 使用统一代理服务
IP资源和服务器资源契合度更高,所以可以直接把请求发送到服务器,由服务器选择可用资源进行转发。客户端需要配置服务器的地址
```
#server统一代理服务
proxyclient.defaultProxy=115.159.40.202:8081
proxyclient.defaultProxy.forceUse=true
```
其中forceUse参数代表强制转发到服务器,默认为false
### 其他
还有一个工具类``com.virjar.dungproxy.client.util.PoolUtil``可以说一说,他提供再httpclient的Context上面操作代理池的功能。
如:
```
  /**
     * 将任意一个代表user的对象绑定到http上下文,只要经过此步骤,对应user基本每次都会被绑定到同一个IP上面。<br/>
     * 适用场景,多个僵尸账户登录目标网站爬取各自所见数据。要求各个用户cookie空间独立, 要求各个账户每次IP保持相同<br/>
     * 注意,IP池根据用户ID的hash值做一致性哈希绑定,请注意userID对象的hashCode函数是否会被均匀散列
     * 
     * @param httpClientContext http的上下文
     * @param userId 代表用户信息的对象
     */
    public static void bindUserKey(HttpClientContext httpClientContext, Object userId) {
        httpClientContext.setAttribute(ProxyConstant.USER_KEY, userId);
    }
```
点点看看



