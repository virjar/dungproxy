using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using proxyipcenter_dotnet.Model;

namespace proxyipcenter_dotnet.Http
{
    public class HttpPackage
    {
        public const int NOTSET = -1;

        private Dictionary<string, string> headers;
        private ProxyInfo proxy;
        private Dictionary<string, string> postFormData;
        private string postBodyData;

        private int requestTimeInMs = NOTSET;

        public HttpPackage addHeader(string key, string value)
        {
            headers.Add(key, value);
            return this;
        }
        

    }
}
