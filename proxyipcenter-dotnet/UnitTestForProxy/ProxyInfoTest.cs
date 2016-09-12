using System;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System.Linq;
using proxyipcenter_dotnet.Model;

namespace UnitTestForProxy
{
    [TestClass]
    public class ProxyInfoTest
    {
        [TestMethod]
        public void GetListTest()
        {
            var lstModel = Proxy.LoadAvailableProxyList();
            Assert.IsNotNull(lstModel);
            Assert.IsTrue(lstModel.Count() == 20);
        }


        

    }
}
