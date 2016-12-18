#使用IpPoolAPI获取IP资源

### 获取IP
获取一个IP的方式是这样的 ``IpPool.getInstance().bind(domain, accessUrl, user);`` 
- 第一个参数是域名,可以传递null,传递null提取accessUrl schema里面的host
- 第二个参数是你当前需要访问的url, 可以为null,为null时domain不能为空
- 第三个参数是当前那个用户再访问,可以为null。这个用户是逻辑上的,主要场景是存在多个账户登录同一个网站进行数据抓取,这个时候希望每个用户再每次请求的时候,尽量(通过一致性hash尽量包装)获取的是同一个IP。当然传入null代表随机绑定IP了。

### 记录IP使用
每当使用IP的时候,需要记录一次IP使用,也就是将IP实例的使用次数加一,用于打分机制计算IP的使用分值,评估IP可用性
方式如下:``com.virjar.dungproxy.client.model.AvProxy.recordUsage``

### 记录IP使用失败
每当IP使用失败的时候,需要记录IP使用失败,也就是将IP实例的失败次数加一,用于打分机制计算IP的使用分值,评估IP可用性
方式如下:``com.virjar.dungproxy.client.model.AvProxy.recordFailed()``

### IP下线
IP下线很简单,拿到IP实例,这样调用``com.virjar.dungproxy.client.model.AvProxy.offline()``。
一般情况不建议这么做,因为IP池会自动检查IP是否应该下线,IP池可以定制各种策略。当时,有些时候IP池的检查机制比较缓慢,而上层业务可以明确知道本IP不可用,这个时候可以使用本API强制下线


### 销毁IP池实例
IP池是单例的,同时里面维护了两个任务线程,在业务完成的时候,需要销毁IP池才能终止内部线程。同时也会执行一些收尾工作,如将可用IP dump。
销毁方式是:``com.virjar.dungproxy.client.ippool.IpPool.destroy``


### 其他
IP池有其他很多扩展点,但是目前接口没有开发完成,待后续完善




