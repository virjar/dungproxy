#proxyipcenter

常见代理端口,我们需要通过端口判断该资源最有可能是那种代理,然后尽早作出资源判定
socket代理:1080
http代理:8123,3128,8080,80

服务器端判断IP匿名性原理
```
 public static String getIpAddress(HttpServletRequest request){  
		String ipAddress = request.getHeader("x-forwarded-for");
		if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getHeader("Proxy-Client-IP");
		}
		if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ipAddress == null || ipAddress.length() == 0 || "unknown".equalsIgnoreCase(ipAddress)) {
			ipAddress = request.getRemoteAddr();
			if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
				// 根据网卡取本机配置的IP
				InetAddress inet = null;
				try {
					inet = InetAddress.getLocalHost();
				} catch (UnknownHostException e) {
					e.printStackTrace();
				}
				ipAddress = inet.getHostAddress();
			}
		}
		// 对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
		if (ipAddress != null && ipAddress.length() > 15) {
			// "***.***.***.***".length() = 15
			if (ipAddress.indexOf(",") > 0) {
				ipAddress = ipAddress.substring(0, ipAddress.indexOf(","));
			}
		}
		return ipAddress;
	}
```

算法原理
CPU调度思想，反馈法

资源位于一个多slot的容器中，一个资源固定时刻只能存在固定的slot内
slot是对资源评分的一个划段,它可能随着资源的score变动而变动,slot数目是优先的,slot代表资源被处理的优先级,类似于CPU反馈调度思想
每次检测都会更新score字段，同时规定收敛幅度大于发散幅度，参数待定，0为收敛最终值

资源抓取原理
框架有一个上层封装的爬虫,它使用url和指定的数据抽取模块来对不同网站进行监控,每个网站都是一个特定的模版文件。如果网站特殊,也可以把抽取逻辑设置成具体的类
抽取结束之后将会返回一个序列json,然后反序列化成java对象

#消重原理
资源入库之前会进行消重,因为爬虫爬取的数据量比较大,入库前消重可以减轻数据库压力。消重使用bloomFilter实现

#unhandle

```
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01//EN">
<html>
  
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <style type="text/css">html,body{height:100%;padding:0;margin:0;}.oc{display:table;width:100%;height:100%;}.ic{display:table-cell;vertical-align:middle;height:100%;}div.msg{display:block;border:1px solid #30c;padding:0;width:500px;font-family:helvetica,sans-serif;margin:10px auto;}h1{font-weight:bold;color:#fff;font-size:14px;margin:0;padding:2px;text-align:center;background: #30c;}p{font-size:12px;margin:15px auto;width:75%;font-family:helvetica,sans-serif;text-align:left;}</style>
    <title>Access Denined</title></head>
  
  <body>
    <div class="oc">
      <div class="ic">
        <div class="msg">
          <h1>Access Denined</h1>
          <p>The maximum web proxy user limit has been reached.</p>
        </div>
      </div>
    </div>
  </body>

</html>
```
```
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html><head><meta http-equiv="Content-Type" content="text/html; charset=iso-8859-1">
<title>ERROR: Gateway Timeout</title></head>
<body><h2>ERROR: Gateway Timeout</h2>
<hr noshade size="1px"/>

<p>While trying to retrieve the URL <a href="http://www.scumall.com/">http://www.scumall.com/</a>:</p>
<ul><li><strong>Connection timed out</strong></li></ul>

<p>Your cache administrator is <a href="mailto:webmaster">webmaster</a>.
<br clear="all"/><hr noshade size="1px"/>
<address>Generated Sat, 27 Aug 2016 17:56:33 GMT by 189.17.105.72 (Mikrotik HttpProxy)</address>
</body></html>
```