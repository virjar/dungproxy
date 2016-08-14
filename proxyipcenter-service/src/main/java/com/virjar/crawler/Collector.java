package com.virjar.crawler;


import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.virjar.crawler.extractor.XmlModeFetcher;
import com.virjar.entity.Proxy;
import com.virjar.repository.ProxyRepository;
import com.virjar.utils.net.DefaultHtmlUnitResultWait;
import com.virjar.utils.net.HttpInvoker;
import com.virjar.utils.net.HttpResult;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.alibaba.fastjson.JSONObject;
public class Collector {

	private int batchsize;

	private URLGenerator urlGenerator;

	private XmlModeFetcher fetcher;
	
	private String charset="utf-8";
	
	private boolean hasdetectcharset = false;
	
	private String website="";
	
	private long lastactivity=0;
	
	private long hibrate=20000;
	
	private boolean javascript= false;
	
	private DefaultHtmlUnitResultWait resultwait;
	
	private Logger logger = Logger.getLogger(Collector.class);
	
	private long getnumber =0;
	
	private String errorinfo="";

	private int failedTimes =0;

	private int sucessTimes =0;

	private boolean useProxy = false;
	

	public String getErrorinfo() {
		return errorinfo;
	}

	public int getBatchsize() {
		return batchsize;
	}

	public void setBatchsize(int batchsize) {
		this.batchsize = batchsize;
	}

	public String getCharset() {
		return charset;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public boolean isHasdetectcharset() {
		return hasdetectcharset;
	}

	public void setHasdetectcharset(boolean hasdetectcharset) {
		this.hasdetectcharset = hasdetectcharset;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public long getHibrate() {
		return hibrate;
	}

	public void setHibrate(long hibrate) {
		this.hibrate = hibrate;
	}

	public boolean isJavascript() {
		return javascript;
	}

	public void setJavascript(boolean javascript) {
		this.javascript = javascript;
	}

	public long getGetnumber() {
		return getnumber;
	}

	public void setGetnumber(long getnumber) {
		this.getnumber = getnumber;
	}


	private Proxy randomChooseOne(List<Proxy> proxies){
		Random random = new Random();
		return proxies.get(random.nextInt(proxies.size()));
	}


	public List<Proxy> newProxy(ProxyRepository proxyRepository){
		int num =0;
		int failedtimes =0;
		String url = "" ;
		List<Proxy> ret = new ArrayList<Proxy>();
		
		if(System.currentTimeMillis() - lastactivity < hibrate){
			return ret;
		}
		lastactivity = System.currentTimeMillis();
		while(num < batchsize){
			try {
				HttpResult result = null;
				url = urlGenerator.newURL();
				HttpInvoker httpInvoker = new HttpInvoker(url);
				if(useProxy){
					List<Proxy> available = proxyRepository.findAvailable();
					if(available.size() >0){
						Proxy proxy = randomChooseOne(available);
						httpInvoker.setproxy(proxy.getIp(),proxy.getPort());
					}
				}
				if(!javascript){
					result = httpInvoker.request();
				}else{
					result = httpInvoker.requestFromHtmlUnit(resultwait);
				}
				if(result.getStatusCode() == 200){
					List<String> fetchresult = fetcher.fetch(result.responseBody);
					if(fetchresult.size() == 0){

					}else{
						failedtimes =0;
						sucessTimes ++;
					}
					for(String str:fetchresult){
						num ++;
						Proxy parseObject = null;
						try {
							parseObject = JSONObject.parseObject(str, Proxy.class);
						} catch (Exception e) {
							e.printStackTrace();
							num --;
							continue;
						}
						parseObject.setAvailbelScore(0L);
						parseObject.setConnectionScore(0L);
						parseObject.setSource(url);
						ret.add(parseObject);
					}
					if(fetchresult.size() ==0){
						urlGenerator.reset();
					}
					Thread.sleep(2000);
					continue;
				}
			} catch (Exception e) {
				logger.error("收集器 "+url+" 错误栈如下：", e);
				errorinfo = url+":"+e ;
				failedTimes ++;
			} 
			failedtimes ++;
			if(failedtimes >3){
				return ret;
			}
		}
		getnumber += ret.size();
		if(ret.size() ==0){
			logger.info("empty resource for :"+url);
		}
		return ret;
	}

	public static List<Collector> buildfromSource(String source) throws DocumentException, IOException{
		if(source == null){
			source = "/handmaper.xml";
		}
		List<Collector> ret = new ArrayList<Collector>();
		Document parseText = DocumentHelper.parseText(IOUtils.toString(Collector.class.getResourceAsStream(source)));

		Element rootElement = parseText.getRootElement();
		if(rootElement != null && "handmapers".equals(rootElement.getName())){
			Iterator<Element> elementIterator = rootElement.elementIterator();
			while(elementIterator.hasNext()){
				Element next = elementIterator.next();
				if("maper".equals(next.getName())){
					try{
						if(!"true".equals(next.element("enable").getTextTrim())){
							continue;
						}
						Collector collecter = new Collector();
						collecter.website = next.elementText("website");
						String hierate = next.elementText("hierate");
						collecter.hibrate = parseTime(hierate);
						
						String usecharset = next.elementText("charset");
						if(usecharset != null){
							collecter.charset = usecharset;
						}
						String bachSize = next.elementText("batchsize");
						collecter.batchsize = 30;
						if(bachSize != null){
							try {
								collecter.batchsize = Integer.parseInt(bachSize);
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						
						String javascriptflag = next.elementText("javasript");
						if("true".equals(javascriptflag)){
							collecter.javascript = true;
							collecter.resultwait = new DefaultHtmlUnitResultWait();
							List<Element> elements = next.elements("minusflag");
							for(Element element:elements){
								collecter.resultwait.addminusflag(element.getTextTrim());
							}
							
							elements = next.elements("plusflag");
							for(Element element:elements){
								collecter.resultwait.addplusflag(element.getTextTrim());
							}
							
						}

						collecter.useProxy = "true".equals(next.elementText("useproxy"));

						collecter.fetcher = new XmlModeFetcher(IOUtils.toString(Collector.class.getResourceAsStream(next.elementText("fetcher"))));
						Element classgenerator = next.element("classgenerator");
						boolean hasagenerator =false;
						if(classgenerator != null &&!"".equals(classgenerator.getTextTrim())){
							Class<? extends URLGenerator> clazz = (Class<? extends URLGenerator>) Collector.class.getClassLoader().loadClass(classgenerator.getTextTrim());
							Constructor<? extends URLGenerator> constructor = clazz.getConstructor();
							collecter.urlGenerator = (URLGenerator) constructor.newInstance();
							hasagenerator = true;
						}
						Element wildcardgenerator = next.element("wildcardgenerator");
						if(!hasagenerator &&  wildcardgenerator!= null && !"".equals(wildcardgenerator.getTextTrim())){
							collecter.urlGenerator = new WildCardURLGenerator(wildcardgenerator.getTextTrim());
							hasagenerator = true;
						}
						if(hasagenerator){
							ret.add(collecter);
						}
					}catch(Exception e){

					}
				}
			}
		}
		return ret;
	}
	
	private static long parseTime(String str){
		if(str == null)
			return 0;
		String newStr = str.toLowerCase();
		try {
			if(newStr.endsWith("h")){
				return Long.parseLong(newStr.substring(0, newStr.length()-1)) * 60 * 60 * 1000;
			}else if(newStr.endsWith("m")){
				return Long.parseLong(newStr.substring(0, newStr.length()-1)) * 60 * 1000;
			}else if(newStr.endsWith("s")){
				return Long.parseLong(newStr.substring(0, newStr.length()-1)) * 1000;
			}else{
				return Long.parseLong(newStr.substring(0, newStr.length()-1));
			}
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0l;
	}

	public int getFailedTimes() {
		return failedTimes;
	}

	public int getSucessTimes() {
		return sucessTimes;
	}
}
