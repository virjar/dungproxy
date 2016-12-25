package com.virjar.dungproxy.server.crawler;

public abstract class URLGenerator {

	protected String baseURL;
	protected int nowPage;
	protected int totallPage;
	public abstract String newURL();

	protected  int maxPage = Integer.MAX_VALUE;


	public void setMaxPage(int maxPage) {
		this.maxPage = maxPage;
	}

	public URLGenerator(){
		nowPage =1;
		totallPage =0;
	}
	
	public void reset(){
		nowPage =1;
		totallPage =0;
	}
}
