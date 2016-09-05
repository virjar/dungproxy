using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace proxyipcenter_dotnet
{
    public class ProxyipCenterClicent
    {

        /// <summary>
        /// 服务所在地址IP
        /// </summary>
        public string ProxyipCenterHostIP { get; set; }

        /// <summary>
        /// 服务所在域名
        /// </summary>
        public string ProxyipCenterDomain { get; set; }


        public ProxyipCenterClicent(string hostIP="", string domain="")
        {
            ProxyipCenterHostIP = hostIP;
            ProxyipCenterDomain = domain;
        }

    }
}
