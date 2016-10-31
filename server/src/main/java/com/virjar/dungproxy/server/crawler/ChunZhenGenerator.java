package com.virjar.dungproxy.server.crawler;

public class ChunZhenGenerator extends URLGenerator {

	private int nowpage=1;
	@Override
	public String newURL() {
		// TODO Auto-generated method stub
		if(nowpage==1){
			nowpage ++;
			return "http://www.cz88.net/proxy/index.shtml";
		}
		if(nowpage ==10){
			nowpage=1;
			return "http://www.cz88.net/proxy/http_10.shtml";
		}
		return "http://www.cz88.net/proxy/http_"+nowpage+++".shtml";
	}

}
