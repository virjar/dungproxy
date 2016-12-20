using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Text;
using System.Threading.Tasks;

namespace proxyipcenter_dotnet
{
    public class ProxyipCenterClicent
    {

        /// <summary>
        /// 服务所在地址IP
        /// </summary>
        public static string ProxyipCenterHostIP { get; set; }

        /// <summary>
        /// 服务所在域名
        /// </summary>
        public static string ProxyipCenterDomain { get; set; }

        public static HttpWebResponse GetHttpWebResponseByProxy(HttpWebRequest httpWebRequest)
        {
            httpWebRequest.Proxy = new WebProxy()
            {
                //获取可用代理
                Address = new Uri(""),
            };
            HttpWebResponse webResponse = (System.Net.HttpWebResponse)httpWebRequest.GetResponse();
            //把返回结果扔到队列中，用于反馈到服务端
            return webResponse;
        }


    }
}
