package com.virjar.scheduler;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.mantou.proxyservice.clean.Cleaner;
import com.mantou.proxyservice.collection.Collecter;
import com.mantou.proxyservice.proxeservice.entity.Proxy;
import com.mantou.proxyservice.proxeservice.repository.ProxyRepository;
import com.mantou.proxyservice.proxeservice.service.ProxyService;

public class CollecterTask implements Runnable {

	@Autowired
	private ProxyService proxyService;

	@Autowired
	private ProxyRepository proxyRepository;
	static Logger logger = Logger.getLogger(CollecterTask.class);
	
	private ThreadPoolTaskExecutor executor;
	
	public ThreadPoolTaskExecutor getExecutor() {
		return executor;
	}

	public void setExecutor(ThreadPoolTaskExecutor executor) {
		this.executor = executor;
	}

	public static List<Collecter> getCollecters() {
		return collecters;
	}


	static List<Collecter> collecters = null;
	static{
		try {
			logger.info("start collecter");
			collecters = Collecter.buildfromSource("/handmaper.xml");
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		        /*
         * int i =0; if(i==0) return ;
         */
		Collections.sort(collecters, new Comparator<Collecter>() {
			@Override
			public int compare(Collecter o1, Collecter o2) {//失败次数越多，被调度的可能性越小。成功的次数越多，被调度的可能性越小。没有成功也没有失败的，被调度的可能性最大
				return (o1.getFailedTimes() * 10 - o1.getSucessTimes() *3) - (o2.getFailedTimes() * 10 - o2.getSucessTimes() *3);
			}
		});
		for (Collecter collecter : collecters) {
            try {
                executor.execute(new WebsiteCollect(collecter));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                logger.error(e);
            }
        }
	}

	private class WebsiteCollect implements Runnable{

		private Collecter collecter;
		
		public WebsiteCollect(Collecter collecter) {
			super();
			this.collecter = collecter;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			List<Proxy> draftproxys = collecter.newProxy(proxyRepository);
			Cleaner.filter(draftproxys);
			proxyService.save(draftproxys);
			
		}
		
	}
}
