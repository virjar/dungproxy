package com.virjar.utils;

import java.io.IOException;
import java.util.Properties;

public class SysConfig {

	private  String server;
	private String appname;
	private String keyverifyurl;
	private SysConfig(){
		load();
	}
	
	private static SysConfig instance;
	static{
		instance = new SysConfig();
	}
	public static SysConfig getInstance(){
		return instance;
	}
	
	public  void load(){
		//ResourceBundle bundle = ResourceBundle.getBundle("/config.propertis",Locale.getDefault(),Thread.currentThread().getContextClassLoader());
		try {
			Properties properties = new Properties();
			properties.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("config.propertis"));
			server = properties.getProperty("servername");
			appname = properties.getProperty("appname","");
			keyverifyurl = properties.getProperty("keyverifyurl");
			System.out.println(server);
			System.out.println(appname);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]){
		new SysConfig().load();
		String keysourceurl = "http://"+SysConfig.getInstance().getServer()+SysConfig.getInstance().getAppname()+"/system/key";
		System.out.println(keysourceurl);
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public String getAppname() {
		return appname;
	}

	public void setAppname(String appname) {
		this.appname = appname;
	}

	public String getKeyverifyurl() {
		return keyverifyurl;
	}
}
