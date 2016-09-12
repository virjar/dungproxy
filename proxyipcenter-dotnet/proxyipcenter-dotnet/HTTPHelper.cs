using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;

namespace proxyipcenter_dotnet
{
    public class HTTPHelper
    {
        /// <summary>
        /// 获取HTML
        /// </summary>
        /// <param name="url"></param>
        /// <returns></returns>
        public static string GetHTMLByURL(string url)
        {
            string htmlCode = string.Empty;
            try
            {
                HttpWebRequest webRequest = (System.Net.HttpWebRequest)System.Net.WebRequest.Create(url);
                webRequest.Timeout = 30000;
                webRequest.Method = "GET";
                webRequest.UserAgent = "Mozilla/4.0";
                webRequest.Headers.Add("Accept-Encoding", "gzip, deflate");
                HttpWebResponse webResponse = (System.Net.HttpWebResponse)webRequest.GetResponse();
                //获取目标网站的编码格式
                string contentype = webResponse.Headers["Content-Type"];
                Regex regex = new Regex("charset\\s*=\\s*[\\W]?\\s*([\\w-]+)", RegexOptions.IgnoreCase);
                if (webResponse.ContentEncoding.ToLower() == "gzip")//如果使用了GZip则先解压
                {
                    using (System.IO.Stream streamReceive = webResponse.GetResponseStream())
                    {
                        using (var zipStream = new System.IO.Compression.GZipStream(streamReceive, System.IO.Compression.CompressionMode.Decompress))
                        {
                            //匹配编码格式
                            if (regex.IsMatch(contentype))
                            {
                                Encoding ending = Encoding.GetEncoding(regex.Match(contentype).Groups[1].Value.Trim());
                                using (StreamReader sr = new System.IO.StreamReader(zipStream, ending))
                                {
                                    htmlCode = sr.ReadToEnd();
                                }
                            }
                            else
                            {
                                using (StreamReader sr = new System.IO.StreamReader(zipStream, Encoding.UTF8))
                                {
                                    htmlCode = sr.ReadToEnd();
                                }
                            }
                        }
                    }
                }
                else
                {
                    using (System.IO.Stream streamReceive = webResponse.GetResponseStream())
                    {
                        var encoding = Encoding.Default;
                        if (contentype.Contains("utf"))
                            encoding = Encoding.UTF8;
                        using (System.IO.StreamReader sr = new System.IO.StreamReader(streamReceive, encoding))
                        {
                            htmlCode = sr.ReadToEnd();
                        }

                    }
                }
                return htmlCode;
            }
            catch (Exception ex)
            {
                return "";
            }
        }

        /// <summary>
        /// 通告代理返回HttpWebResponse
        /// </summary>
        /// <param name="url"></param>
        /// <param name="proxyIP"></param>
        /// <returns></returns>
        public static HttpWebResponse GetHttpWebResponseByProxy(string url,string proxyIP)
        {
            HttpWebRequest webRequest = (System.Net.HttpWebRequest)System.Net.WebRequest.Create(url);
            webRequest.Timeout = 30000;
            webRequest.Method = "GET";
            webRequest.UserAgent = "Mozilla/4.0";
            webRequest.Headers.Add("Accept-Encoding", "gzip, deflate");

            HttpWebResponse webResponse = (System.Net.HttpWebResponse)webRequest.GetResponse();
            webRequest.Proxy = new WebProxy()
            {
                Address = new Uri(proxyIP),
            };
            return webResponse;
        }
    }
}
