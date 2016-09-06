using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Configuration;
using MySql.Data.MySqlClient;
using System.Data;

namespace proxyipcenter_dotnet
{
    public class MyDBHelper
    {
        // public static readonly string connstr =  ConfigurationManager.ConnectionStrings["<%=ConnectionStringName%>"].ConnectionString;
        public static readonly string connstr = "Server=115.159.40.202;Port=3306;Database=proxyipcenter; User=root;Password=0516;";

        #region 执行Command.ExecuteNonQuery(),返回受影响的行数
        /// <summary>
        /// 执行Command.ExecuteNonQuery(),返回受影响的行数
        /// </summary>
        /// <param name="cmdText">执行的语句</param>
        /// <param name="parameters">params传入的参数null</param>
        /// <returns></returns>         
        public static int ExecuteNonQuery(string cmdText, params MySqlParameter[] parameters)
        {
            using (MySqlConnection conn = new MySqlConnection(connstr))
            {
                int result = -1;
                conn.Open();
                using (MySqlCommand cmd = conn.CreateCommand())
                {
                    cmd.CommandText = cmdText;
                    if (parameters == null)
                        result = cmd.ExecuteNonQuery();
                    else
                    {
                        cmd.Parameters.AddRange(parameters);
                        result = cmd.ExecuteNonQuery();
                        cmd.Parameters.Clear();
                    }
                    return result;
                }
            }
        }
        #endregion

        #region 执行Command.ExecuteScalar(),返回首行首列
        /// <summary>
        /// 执行Command.ExecuteScalar(),返回首行首列
        /// </summary>
        /// <param name="cmdText">执行的语句</param>
        /// <param name="parameters">params传入的参数null</param>
        /// <returns></returns>    
        public static object ExecuteScalar(string cmdText,
            params MySqlParameter[] parameters)
        {
            using (MySqlConnection conn = new MySqlConnection(connstr))
            {
                object obj = null;
                conn.Open();
                using (MySqlCommand cmd = conn.CreateCommand())
                {
                    cmd.CommandText = cmdText;
                    if (parameters == null)
                        obj = cmd.ExecuteScalar();
                    else
                    {
                        cmd.Parameters.AddRange(parameters);
                        //obj=cmd.ExecuteScalar();
                        obj = cmd.ExecuteNonQuery();
                        cmd.Parameters.Clear();
                    }
                    return obj;
                }
            }
        }
        #endregion

        #region 执行MySqlDataAdapter,返回DataTable
        /// <summary>
        /// 执行MySqlDataAdapter,返回DataTable
        /// </summary>
        /// <param name="cmdText">执行的语句</param>
        /// <param name="parameters">params传入的参数null</param>
        /// <returns></returns>    
        public static DataTable ExecuteDataTable(string cmdText, params MySqlParameter[] parameters)
        {
            using (MySqlConnection conn = new MySqlConnection(connstr))
            {
                DataSet ds = new DataSet();
                conn.Open();
                using (MySqlCommand cmd = conn.CreateCommand())
                {
                    cmd.CommandText = cmdText;
                    if (parameters != null)
                    {
                        cmd.Parameters.AddRange(parameters);
                        using (MySqlDataAdapter adapter = new MySqlDataAdapter(cmd))
                        {
                            adapter.Fill(ds);
                        }
                        cmd.Parameters.Clear();
                    }
                    else
                    {
                        using (MySqlDataAdapter adapter = new MySqlDataAdapter(cmd))
                        {
                            adapter.Fill(ds);
                        }
                    }
                    return ds.Tables[0];
                }
            }
        }
        #endregion

        #region 执行MySqlDataAdapter SQL语句,返回DataTable
        /// <summary>
        /// 执行MySqlDataAdapter SQL语句,返回DataTable
        /// </summary>
        /// <param name="cmdText">SQL语句</param>
        /// <returns></returns>
        public static DataTable ExecuteDataTable(string cmdText)
        {
            using (MySqlConnection conn = new MySqlConnection(connstr))
            {
                DataSet ds = new DataSet();
                conn.Open();
                using (MySqlCommand cmd = conn.CreateCommand())
                {
                    cmd.CommandText = cmdText;
                    {
                        using (MySqlDataAdapter adapter = new MySqlDataAdapter(cmd))
                        {
                            adapter.Fill(ds);
                        }
                    }
                    return ds.Tables[0];
                }
            }
        }
        #endregion

        #region 执行ExecuteReader,返回MySqlDataReader
        /// <summary>
        /// 执行ExecuteReader,返回MySqlDataReader
        /// </summary>
        /// <param name="cmdText">执行的语句</param>
        /// <param name="parameters">params传入的参数null</param>
        /// <returns></returns>
        public static MySqlDataReader ExecuteDataReader(string cmdText, params MySqlParameter[] parameters)
        {
            MySqlConnection conn = new MySqlConnection(connstr);
            MySqlDataReader read = null;
            conn.Open();
            using (MySqlCommand cmd = conn.CreateCommand())
            {
                cmd.CommandText = cmdText;
                if (parameters != null)
                {
                    cmd.Parameters.AddRange(parameters);
                    read = cmd.ExecuteReader(CommandBehavior.CloseConnection);
                    cmd.Parameters.Clear();
                }
                else
                {
                    read = cmd.ExecuteReader(CommandBehavior.CloseConnection);
                }
                return read;
            }
        }
        #endregion


        #region 返回查询后的数据表第一行DataRow
        /// <summary>
        /// 返回查询后的数据表第一行DataRow
        /// </summary>
        /// <param name="cmdText">执行语句</param>
        /// <param name="parameters">params参数集合</param>
        public DataRow GetDataRow(string cmdText, params MySqlParameter[] parameters)
        {
            DataTable dt = ExecuteDataTable(cmdText, parameters);
            if (dt.Rows.Count > 0)
                return dt.Rows[0];
            else
                return null;

        }
        #endregion

        #region 执行存储过程 ExecuteNonQuery(),返回受影响的行数
        /// <summary>
        /// 执行存储过程 ExecuteNonQuery(),返回受影响的行数
        /// </summary>
        /// <param name="cmdText">执行的语句</param>
        /// <param name="parameters">params传入的参数null</param>
        /// <returns></returns> 
        public static int ExecuteStoredProcedure(string procName, params MySqlParameter[] parameters)
        {
            MySqlConnection conn = new MySqlConnection(connstr);
            int result = -1;
            conn.Open();
            using (MySqlCommand cmd = conn.CreateCommand())
            {
                cmd.CommandText = procName;
                cmd.CommandType = CommandType.StoredProcedure;
                if (parameters != null)
                {
                    cmd.Parameters.AddRange(parameters);
                    result = cmd.ExecuteNonQuery();
                    cmd.Parameters.Clear();
                }
                else
                {
                    cmd.Parameters.AddRange(parameters);
                    result = cmd.ExecuteNonQuery();
                }
                return result;
            }
        }
        #endregion
    }

}
