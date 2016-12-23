#常见问题相关

### 我自己之前的系统存在IP管理模块,现在只想导入IP数据,不想使用dungproxy客户端
访问如下接口获取IP数据 http://proxy.scumall.com:8080/proxyipcenter/allAv 本接口数据实时可用,但是也需要自己做进一步的使用

### 我使用selenium作为访问访问层,也想接入代理池
dungproxy没有直接支持selenium,不过可以直接操作代理池对象。com.virjar.dungproxy.client.ippool.IpPool。
参考webCollector集成代理池实现方案:com.virjar.dungproxy.client.webcollector.DungproxyHttpRequest

### 我的项目对httpclient进行了封装,我不能使用dungproxy封装的httpclient了,但是也想拥有代理池的特性
参考文档 [http里面使用IP池](httpclient.md),通过httpclient插件植入原生httpclient

### 为什么IP池使用了之后,大量请求超时
- 如果希望刚刚使用IP池的时候就超时较少,请先执行预热操作。否则IP池刚刚工作,IP数据没有加载,将不会使用代理
- 请确认IP池IP缓存文件IP数量合理,一般来说IP数量需要大致多余线程数即可。太多导致IP下线不灵敏,换血慢
- 如果还是超时严重,可以尝试运行一段时间,等待IP池统计各个IP使用状况,进行优先级调整,以及执行上下线动作。让IP池资源达到良好状态

### 为什么使用了dungproxy,仍然没有挂上代理
- 请确认执行预热,并且程序预热配置正确只想预热结果文件
- 请确认预热的URL域名和需要代理的网站的域名完全吻合(不支持子域名)
- 请确认配置了预热规则。也即最小配置里面的whiteList
