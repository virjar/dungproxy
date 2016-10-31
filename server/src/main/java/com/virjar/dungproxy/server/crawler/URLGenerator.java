package com.virjar.dungproxy.server.crawler;

public abstract class URLGenerator {

	protected String baseURL;
	protected int nowPage;
	protected int totallPage;
	public abstract String newURL();
	
	public URLGenerator(){
		nowPage =1;
		totallPage =0;
	}
	
	public void reset(){
		nowPage =1;
		totallPage =0;
	}
}
