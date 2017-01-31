## 异步HttpClient支持

除了对apache的封装支持,还对ning httpclient提供支持。用来实现自动代理的异步httpclient请求,方式如下:
```
new AsyncHttpClient(new DungProxyAsyncHttpProvider(null, clientConfig), clientConfig);
```

在建立AsyncHttpClient的时候,传入dungProxy所定制的HttpProvider,ningHttpclient没有apacheHttpClient的HttpClientContext的概念,可以存储本次请求的扩展数据。所以通过定制provider实现,他对Request和handler进行了增强。主要实现自动绑定代理以及拦截IO异常,实现IP打分和上下钱自动化
