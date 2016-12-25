package com.virjar.dungproxy.server.crawler.urlgenerator;

import com.virjar.dungproxy.server.crawler.URLGenerator;

public class WildCardURLGenerator extends URLGenerator {

	private String wildcard;
	
	public WildCardURLGenerator(String wildcard) {
		super();
		this.wildcard = wildcard;
	}

	@Override
	public String newURL() {
		if(nowPage > maxPage){
			reset();
		}
		return wildcard.replaceAll("#\\{autoincreament\\}", nowPage+++"");
	}
}
