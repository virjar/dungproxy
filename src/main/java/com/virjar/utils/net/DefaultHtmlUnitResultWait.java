package com.virjar.utils.net;

import java.util.ArrayList;
import java.util.List;

public class DefaultHtmlUnitResultWait implements HtmlUnitResultWait {

	
	private long timeout;
	private List<String> minusflag = new ArrayList<String>();
	private List<String> plusflag = new ArrayList<String>();
	
	public  DefaultHtmlUnitResultWait(){
		timeout = 2 *60 *1000;
	}
	
	public void addminusflag(String minusflag){
		this.minusflag.add(minusflag);
	}
	
	public void addplusflag(String plusflag){
		this.plusflag.add(plusflag);
	}
	@Override
	public void setTimeOut(long timeOut) {
		// TODO Auto-generated method stub
		this.timeout = timeOut;
	}

	@Override
	public long getTimeOut() {
		// TODO Auto-generated method stub
		return timeout;
	}

	@Override
	public boolean canReturn(String html, String url) {
		// TODO Auto-generated method stub
		for(String flag:plusflag){
			if(!html.contains(flag))
				return false;
		}
		
		for(String flag:minusflag){
			if(html.contains(flag))
				return false;
		}
		return true;
	}

	@Override
	public int getExcuteCode() {
		// TODO Auto-generated method stub
		return 0;
	}

}
