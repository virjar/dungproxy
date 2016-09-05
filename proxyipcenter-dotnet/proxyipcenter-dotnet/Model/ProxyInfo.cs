using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace proxyipcenter_dotnet.Model
{
    public class ProxyInfo
    {
        #region 属性

        /// <summary>
        /// 主鍵
        /// </summary>
        public long id;

        /// <summary>
        /// IP地址
        /// </summary>
        public string ip;

        /// <summary>
        /// 代理IP，也就是目标网站最终看到的IP（多级代理的情况ip和proxy_ip不会相同）
        /// </summary>
        public string proxy_ip;

        /// <summary>
        /// 端口号
        /// </summary>
        public int port;

        /// <summary>
        /// ip的数字表示，用于过滤连续IP问题
        /// </summary>
        public long ip_value;

        /// <summary>
        /// 国家
        /// </summary>
        public string country; 


        /// <summary>
        /// 地区
        /// </summary>
        public string area;

        /// <summary>
        /// 省
        /// </summary>
        public string region;

        /// <summary>
        /// 市
        /// </summary>
        public string city; 

        /// <summary>
        /// 运营商
        /// </summary>
        public string isp;

        /// <summary>
        /// 国家代码
        /// </summary>
        public string country_id;

        /// <summary>
        /// 地区代码
        /// </summary>
        public string area_id; 

        /// <summary>
        /// 省级代码
        /// </summary>
        public string region_id; 

        /// <summary>
        /// 城市代码
        /// </summary>
        public string city_id; 

        /// <summary>
        /// isp代码
        /// </summary>
        public string isp_id;

        /// <summary>
        /// 地理位置ID，融合各个地理位置获取的一个数字，数值约接近表示实际地理位置约接近
        /// </summary>
        public long address_id; 

        /// <summary>
        /// 透明度(高匿，普通，透明)
        /// </summary>
        public int transperent;

        /// <summary>
        /// 连接时间（越小速度越快）
        /// </summary>
        public long speed;

        /// <summary>
        /// 类型（http，https,httpAndHttps,socket,qq）
        /// </summary>
        public long type; 

        /// <summary>
        /// 类型（http，https,httpAndHttps,socket,qq）
        /// </summary>
        public long connection_score;

        /// <summary>
        /// 可用性打分
        /// </summary>
        public long availbel_score;

        /// <summary>
        /// 连接性打分时间
        /// </summary>
        public DateTime connection_score_date;

        /// <summary>
        /// 可用性打分时间
        /// </summary>
        public DateTime availbel_score_date; 

        /// <summary>
        /// 收录时间
        /// </summary>
        public DateTime createtime;

        /// <summary>
        /// 是否支持翻墙
        /// </summary>
        public bool support_gfw; 

        /// <summary>
        /// 翻墙访问速度
        /// </summary>
        public long gfw_speed;

        /// <summary>
        /// 资源来源url
        /// </summary>
        public string source; 

        /// <summary>
        /// 爬虫key，用户统计爬虫收集分布
        /// </summary>
        public string crawler_key;

        #endregion

        public static List<ProxyInfo> LoadProxyInfo(int pageSum=0)
        {
            if(pageSum==0)
            {
                pageSum = 10;
            }
            return new List<ProxyInfo>();

        }
    
    }
}
