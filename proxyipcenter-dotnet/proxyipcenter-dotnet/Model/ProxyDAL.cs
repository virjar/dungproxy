//============================================================
//http://codelover.link author:李国宝
//============================================================

using System;
using System.Collections.Generic;
using System.Text;
using System.Data;
using MySql.Data.MySqlClient;
using proxyipcenter_dotnet.Model;

namespace proxyipcenter_dotnet.DAL
{
	public partial class ProxyDAL
	{
        #region 根据传入Model，并返回Model
        /// <summary>
        /// 根据传入Model，并返回Model
        /// </summary>        
        public bool Add (Proxy proxy)
		{
				string sql ="INSERT INTO proxy (ip, proxy_ip, port, ip_value, country, area, region, city, isp, country_id, area_id, region_id, city_id, isp_id, address_id, transperent, speed, type, connection_score, availbel_score, connection_score_date, availbel_score_date, createtime, support_gfw, gfw_speed, source, crawler_key)  VALUES (@ip, @proxy_ip, @port, @ip_value, @country, @area, @region, @city, @isp, @country_id, @area_id, @region_id, @city_id, @isp_id, @address_id, @transperent, @speed, @type, @connection_score, @availbel_score, @connection_score_date, @availbel_score_date, @createtime, @support_gfw, @gfw_speed, @source, @crawler_key)";
				MySqlParameter[] para = new MySqlParameter[]
					{
						new MySqlParameter("@ip", ToDBValue(proxy.ip)),
						new MySqlParameter("@proxy_ip", ToDBValue(proxy.proxy_ip)),
						new MySqlParameter("@port", ToDBValue(proxy.port)),
						new MySqlParameter("@ip_value", ToDBValue(proxy.ip_value)),
						new MySqlParameter("@country", ToDBValue(proxy.country)),
						new MySqlParameter("@area", ToDBValue(proxy.area)),
						new MySqlParameter("@region", ToDBValue(proxy.region)),
						new MySqlParameter("@city", ToDBValue(proxy.city)),
						new MySqlParameter("@isp", ToDBValue(proxy.isp)),
						new MySqlParameter("@country_id", ToDBValue(proxy.country_id)),
						new MySqlParameter("@area_id", ToDBValue(proxy.area_id)),
						new MySqlParameter("@region_id", ToDBValue(proxy.region_id)),
						new MySqlParameter("@city_id", ToDBValue(proxy.city_id)),
						new MySqlParameter("@isp_id", ToDBValue(proxy.isp_id)),
						new MySqlParameter("@address_id", ToDBValue(proxy.address_id)),
						new MySqlParameter("@transperent", ToDBValue(proxy.transperent)),
						new MySqlParameter("@speed", ToDBValue(proxy.speed)),
						new MySqlParameter("@type", ToDBValue(proxy.type)),
						new MySqlParameter("@connection_score", ToDBValue(proxy.connection_score)),
						new MySqlParameter("@availbel_score", ToDBValue(proxy.availbel_score)),
						new MySqlParameter("@connection_score_date", ToDBValue(proxy.connection_score_date)),
						new MySqlParameter("@availbel_score_date", ToDBValue(proxy.availbel_score_date)),
						new MySqlParameter("@createtime", ToDBValue(proxy.createtime)),
						new MySqlParameter("@support_gfw", ToDBValue(proxy.support_gfw)),
						new MySqlParameter("@gfw_speed", ToDBValue(proxy.gfw_speed)),
						new MySqlParameter("@source", ToDBValue(proxy.source)),
						new MySqlParameter("@crawler_key", ToDBValue(proxy.crawler_key)),
					};
					
				int AddId = (int)MyDBHelper.ExecuteScalar(sql, para);
				if(AddId==1)
				{
					return true;
				}else
				{
					return false;					
				}
		}
         #endregion

        #region  根据Id删除数据记录
        /// <summary>
        /// 根据Id删除数据记录
        /// </summary>
        public int DeleteById(long id)
		{
            string sql = "DELETE from proxy WHERE Id = @Id";

            MySqlParameter[] para = new MySqlParameter[]
			{
				new MySqlParameter("@id", id)
			};
		
            return MyDBHelper.ExecuteNonQuery(sql, para);
		}
		 #endregion
		
				

		
        #region 根据传入Model更新数据并返回更新后的Model
        /// <summary>
        /// 根据传入Model更新数据并返回更新后的Model
        /// </summary>
        public int Update(Proxy proxy)
        {
            string sql =
                "UPDATE proxy " +
                "SET " +
			" ip = @ip" 
                +", proxy_ip = @proxy_ip" 
                +", port = @port" 
                +", ip_value = @ip_value" 
                +", country = @country" 
                +", area = @area" 
                +", region = @region" 
                +", city = @city" 
                +", isp = @isp" 
                +", country_id = @country_id" 
                +", area_id = @area_id" 
                +", region_id = @region_id" 
                +", city_id = @city_id" 
                +", isp_id = @isp_id" 
                +", address_id = @address_id" 
                +", transperent = @transperent" 
                +", speed = @speed" 
                +", type = @type" 
                +", connection_score = @connection_score" 
                +", availbel_score = @availbel_score" 
                +", connection_score_date = @connection_score_date" 
                +", availbel_score_date = @availbel_score_date" 
                +", createtime = @createtime" 
                +", support_gfw = @support_gfw" 
                +", gfw_speed = @gfw_speed" 
                +", source = @source" 
                +", crawler_key = @crawler_key" 
               
            +" WHERE id = @id";


			MySqlParameter[] para = new MySqlParameter[]
			{
				new MySqlParameter("@id", proxy.id)
					,new MySqlParameter("@ip", ToDBValue(proxy.ip))
					,new MySqlParameter("@proxy_ip", ToDBValue(proxy.proxy_ip))
					,new MySqlParameter("@port", ToDBValue(proxy.port))
					,new MySqlParameter("@ip_value", ToDBValue(proxy.ip_value))
					,new MySqlParameter("@country", ToDBValue(proxy.country))
					,new MySqlParameter("@area", ToDBValue(proxy.area))
					,new MySqlParameter("@region", ToDBValue(proxy.region))
					,new MySqlParameter("@city", ToDBValue(proxy.city))
					,new MySqlParameter("@isp", ToDBValue(proxy.isp))
					,new MySqlParameter("@country_id", ToDBValue(proxy.country_id))
					,new MySqlParameter("@area_id", ToDBValue(proxy.area_id))
					,new MySqlParameter("@region_id", ToDBValue(proxy.region_id))
					,new MySqlParameter("@city_id", ToDBValue(proxy.city_id))
					,new MySqlParameter("@isp_id", ToDBValue(proxy.isp_id))
					,new MySqlParameter("@address_id", ToDBValue(proxy.address_id))
					,new MySqlParameter("@transperent", ToDBValue(proxy.transperent))
					,new MySqlParameter("@speed", ToDBValue(proxy.speed))
					,new MySqlParameter("@type", ToDBValue(proxy.type))
					,new MySqlParameter("@connection_score", ToDBValue(proxy.connection_score))
					,new MySqlParameter("@availbel_score", ToDBValue(proxy.availbel_score))
					,new MySqlParameter("@connection_score_date", ToDBValue(proxy.connection_score_date))
					,new MySqlParameter("@availbel_score_date", ToDBValue(proxy.availbel_score_date))
					,new MySqlParameter("@createtime", ToDBValue(proxy.createtime))
					,new MySqlParameter("@support_gfw", ToDBValue(proxy.support_gfw))
					,new MySqlParameter("@gfw_speed", ToDBValue(proxy.gfw_speed))
					,new MySqlParameter("@source", ToDBValue(proxy.source))
					,new MySqlParameter("@crawler_key", ToDBValue(proxy.crawler_key))
			};

			return MyDBHelper.ExecuteNonQuery(sql, para);
        }
        #endregion
		
        #region 传入Id，获得Model实体
        /// <summary>
        /// 传入Id，获得Model实体
        /// </summary>
        public Proxy GetById(long id)
        {
            string sql = "SELECT * FROM proxy WHERE Id = @Id";
            using(MySqlDataReader reader = MyDBHelper.ExecuteDataReader(sql, new MySqlParameter("@Id", id)))
			{
				if (reader.Read())
				{
					return ToModel(reader);
				}
				else
				{
					return null;
				}
       		}
        }
		#endregion
        
        #region 把DataRow转换成Model
        /// <summary>
        /// 把DataRow转换成Model
        /// </summary>
		public Proxy ToModel(MySqlDataReader dr)
		{
			Proxy proxy = new Proxy();

			proxy.id = (long)ToModelValue(dr,"id");
			proxy.ip = (string)ToModelValue(dr,"ip");
			proxy.proxy_ip = (string)ToModelValue(dr,"proxy_ip");
			proxy.port = (int?)ToModelValue(dr,"port");
			proxy.ip_value = (long?)ToModelValue(dr,"ip_value");
			proxy.country = (string)ToModelValue(dr,"country");
			proxy.area = (string)ToModelValue(dr,"area");
			proxy.region = (string)ToModelValue(dr,"region");
			proxy.city = (string)ToModelValue(dr,"city");
			proxy.isp = (string)ToModelValue(dr,"isp");
			proxy.country_id = (string)ToModelValue(dr,"country_id");
			proxy.area_id = (string)ToModelValue(dr,"area_id");
			proxy.region_id = (string)ToModelValue(dr,"region_id");
			proxy.city_id = (string)ToModelValue(dr,"city_id");
			proxy.isp_id = (string)ToModelValue(dr,"isp_id");
			proxy.address_id = (long?)ToModelValue(dr,"address_id");
			proxy.transperent = (sbyte?)ToModelValue(dr,"transperent");
			proxy.speed = (long?)ToModelValue(dr,"speed");
			proxy.type = (sbyte?)ToModelValue(dr,"type");
			proxy.connection_score = (long)ToModelValue(dr,"connection_score");
			proxy.availbel_score = (long)ToModelValue(dr,"availbel_score");
			proxy.connection_score_date = (DateTime?)ToModelValue(dr,"connection_score_date");
			proxy.availbel_score_date = (DateTime?)ToModelValue(dr,"availbel_score_date");
			proxy.createtime = (DateTime)ToModelValue(dr,"createtime");
			proxy.support_gfw = (sbyte?)ToModelValue(dr,"support_gfw");
			proxy.gfw_speed = (long?)ToModelValue(dr,"gfw_speed");
			proxy.source = (string)ToModelValue(dr,"source");
			proxy.crawler_key = (string)ToModelValue(dr,"crawler_key");
			return proxy;
		}
		#endregion
        
        #region  获得总记录数
        ///<summary>
        /// 获得总记录数
        ///</summary>        
		public int GetTotalCount()
		{
			string sql = "SELECT count(*) FROM proxy";
			return (int)MyDBHelper.ExecuteScalar(sql);
		}
		#endregion
        
        #region 获得分页记录集IEnumerable<>
        ///<summary>
        /// 获得分页记录集IEnumerable<>
        ///</summary>              
		public IEnumerable<Proxy> GetPagedData(int minrownum,int maxrownum)
		{
			string sql = "SELECT * from(SELECT *,(row_number() over(order by id))-1 rownum FROM proxy) t where rownum>=@minrownum and rownum<=@maxrownum";
			using(MySqlDataReader reader = MyDBHelper.ExecuteDataReader(sql,
				new MySqlParameter("@minrownum",minrownum),
				new MySqlParameter("@maxrownum",maxrownum)))
			{
				return ToModels(reader);					
			}
		}
		#endregion
        
        
        #region 获得总记录集IEnumerable<>
        ///<summary>
        /// 获得总记录集IEnumerable<>
        ///</summary> 
		public IEnumerable<Proxy> GetAll()
		{
			string sql = "SELECT * FROM proxy";
			using(MySqlDataReader reader = MyDBHelper.ExecuteDataReader(sql))
			{
				return ToModels(reader);			
			}
		}
        #endregion
		
        #region 把MySqlDataReader转换成IEnumerable<>
        ///<summary>
        /// 把MySqlDataReader转换成IEnumerable<>
        ///</summary> 
		protected IEnumerable<Proxy> ToModels(MySqlDataReader reader)
		{
			var list = new List<Proxy>();
			while(reader.Read())
			{
				list.Add(ToModel(reader));
			}	
			return list;
		}		
		#endregion
        
        #region 判断数据是否为空
        ///<summary>
        /// 判断数据是否为空
        ///</summary>
		protected object ToDBValue(object value)
		{
			if(value==null)
			{
				return DBNull.Value;
			}
			else
			{
				return value;
			}
		}
		#endregion
        
        #region 判断数据表中是否包含该字段
        ///<summary>
        /// 判断数据表中是否包含该字段
        ///</summary>
		protected object ToModelValue(MySqlDataReader reader,string columnName)
		{
			if(reader.IsDBNull(reader.GetOrdinal(columnName)))
			{
				return null;
			}
			else
			{
				return reader[columnName];
			}
		}
        #endregion


        /// <summary>
        /// 默认一次取出20条数据
        /// </summary>
        /// <param name="pageCount"></param>
        /// <returns></returns>
        public IEnumerable<Proxy> GetAvailableProxyList(int pageCount=0)
        {
            string sql = string.Format("SELECT  * FROM proxyipcenter.proxy where availbel_score >0 order by availbel_score_date desc limit {0};", pageCount != 0 && pageCount < 2000 ?pageCount:20);
           
            using (MySqlDataReader reader = MyDBHelper.ExecuteDataReader(sql))
            {
                return ToModels(reader);
            }
        }

	}
}