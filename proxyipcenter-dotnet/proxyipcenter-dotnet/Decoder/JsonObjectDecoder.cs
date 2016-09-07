//========================
//死宅真可怕 天天沉迷于东方
//========================
using System;
using System.IO;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using proxyipcenter_dotnet.Model;
using System.Text.RegularExpressions;

namespace proxyipcenter_dotnet.Decoder
{
    public class JsonObjectDecoder
    {
        public List<Proxy> ProxyDecode(string input)
        {
            var proxyList = new List<Proxy>();
            var reg = new Regex(@"""data"":\[(.*?)\]");
            var match = reg.Match(input);
            var data = match.Groups[1];

            reg = new Regex(@"\{(.*?)\}");
            match = reg.Match(data.ToString());
            var proxy = match.Groups[1].ToString();
            while (proxy.Length != 0)
            {
                proxyList.Add(ProxyData(proxy));
                match = match.NextMatch();
                proxy = match.Groups[1].ToString();
            }
            return proxyList;
        }
        private Proxy ProxyData(string json)
        {
            var argumentTable = new Dictionary<string, string>();
            var args = json.Split(',');
            foreach (var item in args)
            {
                AddArgument(ref argumentTable, item);
            }

            var proxy = new Proxy();
            foreach (var p in proxy.GetType().GetProperties())
            {
                string value;
                if (argumentTable.TryGetValue(p.Name, out value))
                {
                    var type = p.PropertyType;
                    if (type.IsGenericType&&type.GetGenericTypeDefinition().Equals(typeof(Nullable<>)))
                    {
                        var nullableConverter = new System.ComponentModel.NullableConverter(type);
                        type = nullableConverter.UnderlyingType;
                    }
                    p.SetValue(proxy, Convert.ChangeType(value, type));
                }
            }
            return proxy;
        }
        private void AddArgument(ref Dictionary<string, string> dic, string word)
        {
            var name = (word.Split(':'))[0];
            var value = (word.Split(':'))[1];
            name = name.Substring(1, name.Length - 2);
            if (value.StartsWith("\"")&&value.EndsWith("\""))
            {
                value = value.Substring(1, value.Length - 2);
            }
            dic.Add(name, value);
        }
    }
}
