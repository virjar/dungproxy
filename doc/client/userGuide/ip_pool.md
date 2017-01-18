#使用IpPoolAPI获取IP资源

### 获取IP
获取一个IP的方式是这样的 ``IpPool.getInstance().bind(domain, accessUrl);`` 
- 第一个参数是域名,可以传递null,传递null提取accessUrl schema里面的host
- 第二个参数是你当前需要访问的url, 可以为null,为null时domain不能为空

曾经有一个绑定用户的功能,使得同一个账户每次获取的IP相同,后来觉得是过度设计,因为对于抓取场景,切换IP是很普通的需求,而且貌似没有多少server会检查常用IP。就算有也是小众需求,本框架不必支持

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




