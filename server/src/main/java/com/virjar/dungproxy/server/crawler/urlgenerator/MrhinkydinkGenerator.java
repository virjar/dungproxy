package com.virjar.dungproxy.server.crawler.urlgenerator;

import com.virjar.dungproxy.server.crawler.URLGenerator;

public class MrhinkydinkGenerator extends URLGenerator {

	private int nowpage=1;
	@Override
	public String newURL() {
		// TODO Auto-generated method stub
		if(nowpage==1){
			nowpage ++;
			return "http://www.mrhinkydink.com/proxies.htm";
		}
		if(nowpage == 14){
			nowpage=1;
			return "http://www.mrhinkydink.com/proxies14.htm";
		}
		return "http://www.mrhinkydink.com/proxies"+nowpage+++".htm";
	}

}
