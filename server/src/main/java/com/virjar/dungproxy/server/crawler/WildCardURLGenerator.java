package com.virjar.dungproxy.server.crawler;

public class WildCardURLGenerator extends URLGenerator {

	private String wildcard;
	
	public WildCardURLGenerator(String wildcard) {
		super();
		this.wildcard = wildcard;
	}

	@Override
	public String newURL() {
		// TODO Auto-generated method stub
		return wildcard.replaceAll("#\\{autoincreament\\}", nowPage+++"");
	}
}
