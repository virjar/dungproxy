# 对httpclient的封装,自动注册了代理池

封装Httpclient的原因是,我觉得Build Httpclient都是一个麻烦事儿,所以提供了一个HttpClientBuilder,希望再build的时候,自动把两个组件植入进去。然而发现InnerHttpClient不让我在外面创建,怒了之后把InnerHttpclient的代码给扒出来了。然后我发现可以做的事情很多了,比如默认的User-agent给替换成一个真实浏览器,而不是HttpClient,比如封装常见的访问接口。可以直接传入一个url拿到数据等等。
- CrawlerHttpClientBuilder 我们的httpclient构建器,他会默认注册代理IP池所提供的适配组件,提供自动代理功能。
- CrawlerHttpClient 修改自InnerHttpClient,我在上面做了两件事情,提供常见场景的http请求接口封装,请求条件有多种,返回都是字符串。第二件事是提供了一个字符集探测的功能,这个功能很有必要,我们是一个浏览器身份的客户端,面对的是各种无良网站服务器,他们的字符集种类太多,我们不可能每次都能本地指定目标网站的字符集,特别是有些时候逻辑上都不知道我们访问的是那个网站。
- HttpInvoker 以静态的方式代理CrawlerHttpClient,并实现场景场景的网络请求封装。这样会维护一个Httpclient实例,所有请求都会使用同一个httpclient。~~所以需要注意的是,他不适合多个模拟用户同时访问一个网站,因为所有请求都共用同一个cookie空间。当然为每个用户维护一个CrawlerHttpClient是可以实现多个用户同时在线的。

现在的HttpInvoker也指出多用户cookie空间隔离了,只需要在httpClientContext里面添加一个代表用户的字符串,底层便会自动根据账户名称做路由


需要注意的是HttpInvoker定制了cookieStore,目的是不允许服务器下发的cookie过期时间太久,目前做法是不能超过当前时间之内的一个小时,超过后设置为一个小时。对于HttpInvoker,他是一个静态工具类,所以不能提供多个用户同时访问一个站点的支持,因为任何时刻,他们都会使用同一个cookie空间。这样也不适合需要保持session的场景(单个用户单次业务操作在一个小时之类还是可以)。改造cookieStore的原因是同一个httpclient在使用时间太久之后,会收到服务器下发的各种cookie,也就导致无用cookie过度膨胀,因为服务器不能识别的http 400。

## 使用CrawlerHttpClient
CrawlerHttpClient的使用和普通HttpClient没有区别,只是她需要CrawlerHttpClientBuilder来产生,如下:
```
 private static CrawlerHttpClient crawlerHttpClient;
    static {
        SocketConfig socketConfig = SocketConfig.custom().setSoKeepAlive(true).setSoLinger(-1).setSoReuseAddress(false)
                .setSoTimeout(ProxyConstant.SOCKETSO_TIMEOUT).setTcpNoDelay(true).build();
        X509TrustManager x509mgr = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] xcs, String string) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] xcs, String string) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        SSLContext sslContext = null;
        try {
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { x509mgr }, null);
        } catch (Exception e) {
            //// TODO: 16/11/23
        }

        SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(sslContext,
                SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        crawlerHttpClient = CrawlerHttpClientBuilder.create().setMaxConnTotal(1000).setMaxConnPerRoute(50)
                .setDefaultSocketConfig(socketConfig).setSSLSocketFactory(sslConnectionSocketFactory)
                .setRedirectStrategy(new LaxRedirectStrategy()).setDefaultCookieStore(new BarrierCookieStore()).build();
    }
```
以上代码摘抄自``com.virjar.dungproxy.client.httpclient.HttpInvoker``

CrawlerHttpClient提供的扩展功能:
```
    public static String postJSON(String url, Object entity, Header[] headers) 
    public static String get(String url) 
    public static String get(String url, Charset charset) 
    public static String get(String url, Charset charset, Header[] headers) 
    public static String get(String url, Charset charset, Header[] headers, String proxyIp, int proxyPort) 
    public static String get(String url, Charset charset, String proxyIp, int proxyPort) 
    public static String get(String url, Header[] headers) 
    public static String get(String url, Header[] headers, String proxyIp, int proxyPort) 
    public static String get(String url, HttpClientContext httpClientContext) 
    public static String get(String url, List<NameValuePair> nameValuePairs, Header[] headers, String proxyIp,int proxyPort)
    public static String get(String url, List<NameValuePair> params) 
    public static String get(String url, List<NameValuePair> params, Charset charset) 
    public static String get(String url, List<NameValuePair> params, Charset charset, Header[] headers) 
    public static String get(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp, int proxyPort)
    public static String get(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp, int proxyPort, HttpClientContext httpClientContext)
    public static String get(String url, List<NameValuePair> params, Charset charset, String proxyIp, int proxyPort) 
    public static String get(String url, List<NameValuePair> params, Header[] headers) 
    public static String get(String url, List<NameValuePair> params, String proxyIp, int proxyPort) 
    public static String get(String url, Map<String, String> params, Charset charset, Header[] headers, String proxyIp, int proxyPort)
    public static String get(String url, String proxyIp, int proxyPort) 
    public static int getStatus(String url, String proxyIp, int proxyPort) 
    public static String post(String url, HttpEntity entity, Charset charset, Header[] headers, String proxyIp, int proxyPort)
    public static String post(String url, String entity) 
    public static String post(String url, String entity, Charset charset, Header[] headers, String proxyIp,int proxyPort)
    public static String post(String url, String entity, Header[] headers) 
    public static String post(String url, List<NameValuePair> params) 
    public static String post(String url, List<NameValuePair> params, Charset charset, Header[] headers, String proxyIp,int proxyPort)
    public static String post(String url, List<NameValuePair> params, Header[] headers) 
    public static String post(String url, Map<String, String> params) 
    public static String post(String url, Map<String, String> params, Charset charset, Header[] headers, String proxyIp,int proxyPort)
    public static String post(String url, Map<String, String> params, Header[] headers) 
    public static String postJSON(String url, Object entity) 
    public static String postJSON(String url, Object entity, Charset charset, Header[] headers, String proxyIp,int proxyPort)

```