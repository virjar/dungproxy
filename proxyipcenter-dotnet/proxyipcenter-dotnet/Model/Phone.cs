using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace proxyipcenter_dotnet.Model
{
    [Serializable]
    public class Phone
    {
        private string province;
        private string city;
        private short mmo;

        public string Province
        {
            get
            {
                return province;
            }

            set
            {
                province = value;
            }
        }

        public string City
        {
            get
            {
                return city;
            }

            set
            {
                city = value;
            }
        }

        public short Mmo
        {
            get
            {
                return mmo;
            }

            set
            {
                mmo = value;
            }
        }

        public Phone()
        {

        }

        public Phone(string p,string c,short m)
        {
            province = p;
            city = c;
            mmo = m;
        }

        public override string ToString()
        {
            object[] args = { province, city, mmo };
            return string.Format("Phone [province={0}, city={1}, mmo={2}", args);
        }
    }
}
