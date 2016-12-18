# 讲解预热IP

IP可用性和实际环境关系很大,任何时候,其他机器验证通过的资源在本地都可能验证不通过。他和代理IP到目标网站、客户端到代理服务器都有关系。所以下发的IP资源在本地可能是不可用的,而预热模块则是在业务启动前产生一批契合本地环境的IP资源。

- 如果您接入的IP资源是自己维护的高质量私有代理IP,则可以考虑不进行离线预热操作
- 如果爬取资源不需要稳定性,也可以不进行预热操作。IP池会慢慢调整资源,直到IP质量达到一定状态

## 如何预热

### 配置预热规则
在dungclient(获取dunclient的方式见[编译客户端代码](build_code.md))里面,配置``conf/proxyclient.properties``
```
#对于预热器,配置任务规则
proxyclient.proxyDomainStrategy.whiteList=www.douban.com
proxyclient.DefaultAvProxyDumper.dumpFileName=/Users/virjar/git/proxyipcenter/client/product/availableProxy.json
proxyclient.preHeater.testList=https://www.douban.com/group/explore
proxyclient.preHeater.serialize.step=30
```
- proxyclient.DefaultAvProxyDumper.dumpFileName是一个文件地址,他存放了预热的结果,也就是可用IP缓存。
- proxyclient.preHeater.testList是需要检查的网站的URL,注意需要是GET请求,访问结果是HTTP_OK则认为检查通过。多个URL的话,逗号分割即可如:http://www.baidu.com,https://www.douban.com/group/explore
- proxyclient.preHeater.serialize.step配置增量序列化,是指每当达到一定数目的IP测试通过之后,就将数据序列化一次。实例配置是指,每当30个IP测试通过,就将数据写入到proxyclient.DefaultAvProxyDumper.dumpFileName

### 启动预热脚本

启动在你的操作系统平台上面的启动脚本。preHeater.sh 或者 preHeater.bat
```
bogon:dungclient virjar$ ls
bin	conf	lib
bogon:dungclient virjar$ sh bin/preHeater.sh 
2016-12-18 10:19:38 INFO  [pool-1-thread-4] c.v.d.c.u.IpAvValidator:39>>local IP:192.168.0.100
2016-12-18 10:19:39 INFO  [pool-1-thread-36] c.v.d.c.i.PreHeater$UrlCheckTask:177>>preHeater available test passed for proxy:{"disable":false,"domainPool":{"coreSize":30,"domain":"www.douban.com","isRefreshing":false,"minSize":1,"resourceFacade":{},"smartProxyQueue":{"andAdjustPriority":{"$ref":"$"},"ratio":0.3},"testUrls":[]},"failedCount":0,"init":true,"ip":"1.82.216.135","port":80,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}} for url:https://www.douban.com/group/explore
2016-12-18 10:19:39 INFO  [pool-1-thread-2] c.v.d.c.i.PreHeater$UrlCheckTask:177>>preHeater available test passed for proxy:{"disable":false,"domainPool":{"coreSize":30,"domain":"www.douban.com","isRefreshing":false,"minSize":1,"resourceFacade":{},"smartProxyQueue":{"andAdjustPriority":{"$ref":"$"},"ratio":0.3},"testUrls":[]},"failedCount":0,"init":true,"ip":"183.203.22.167","port":80,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}} for url:https://www.douban.com/group/explore
2016-12-18 10:19:39 INFO  [pool-1-thread-30] c.v.d.c.i.PreHeater$UrlCheckTask:177>>preHeater available test passed for proxy:{"disable":false,"domainPool":{"coreSize":30,"domain":"www.douban.com","isRefreshing":false,"minSize":1,"resourceFacade":{},"smartProxyQueue":{"andAdjustPriority":{"disable":false,"domainPool":{"$ref":"$.domainPool"},"failedCount":0,"init":true,"ip":"1.82.216.135","port":80,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},"ratio":0.3},"testUrls":[]},"failedCount":0,"init":true,"ip":"183.95.80.168","port":80,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}} for url:https://www.douban.com/group/explore
...
```
全部预热动作完成大概需要一个半到两个小时,如果你觉得不需要这么多IP资源,可以在中途停止任务。
或者你也可以后台启动任务 ``nohup sh bin/preHeater.sh &``

### 查看或者编辑预热结果
在你配置的文件地址出,将会产生对应的文件,里面以json(默认序列化类)的结构存储数据,你可以编辑这个文件,只要编辑结果符合json规范即可。
```
bogon:dungclient virjar$ cat /Users/virjar/git/proxyipcenter/client/product/availableProxy.json
{"www.douban.com":[{"disable":false,"failedCount":0,"init":true,"ip":"218.90.174.167","port":3128,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"222.186.161.215","port":3128,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"223.68.1.38","port":8000,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"58.217.195.141","port":80,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"1.82.216.135","port":80,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"114.212.160.2","port":80,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"210.245.25.228","port":3128,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"183.203.22.167","port":80,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"183.95.80.168","port":80,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"120.24.102.19","port":80,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"210.245.25.228","port":8080,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"183.131.151.208","port":80,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"121.14.9.76","port":80,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"103.4.166.203","port":8080,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"103.28.149.118","port":8080,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"187.44.1.54","port":8080,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"115.231.219.202","port":3128,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"120.24.73.165","port":3128,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"203.156.126.55","port":3129,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"114.215.150.13","port":3128,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"123.30.238.16","port":3128,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"120.92.3.127","port":80,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"221.186.117.250","port":80,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"190.102.151.235","port":8080,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"94.20.21.38","port":8888,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"218.56.132.154","port":8080,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"94.42.140.10","port":8080,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"123.57.180.234","port":3128,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}},{"disable":false,"failedCount":0,"init":true,"ip":"222.201.132.32","port":3128,"referCount":0,"score":{"avgScore":0,"failedCount":0,"referCount":0}}]}bogon:dungclient virjar$ 
```