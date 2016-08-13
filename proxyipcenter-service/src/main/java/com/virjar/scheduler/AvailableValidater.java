package com.virjar.scheduler;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.mantou.proxyservice.proxeservice.entity.Proxy;
import com.mantou.proxyservice.proxeservice.service.ProxyService;
import com.mantou.proxyservice.utils.ProxyUtil;

public class AvailableValidater implements Runnable {
	@Autowired
	private ProxyService proxyService;

	static Logger logger = Logger.getLogger(AvailableValidater.class);

	private ThreadPoolTaskExecutor executor;

	public ThreadPoolTaskExecutor getExecutor() {
		return executor;
	}

	public void setExecutor(ThreadPoolTaskExecutor executor) {
		this.executor = executor;
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			List<Proxy> needupdate = proxyService.find4availableupdate();
			//needupdate.clear();
			if(needupdate.size() ==0)
			{
				logger.info("no proxy need to update");
				System.out.println("no proxy need to update");
				return;
			}
			for(Proxy proxy:needupdate){
				executor.execute(new ProxyAvailableTester(proxy));
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.error("error", e);
		}
	}
	private class ProxyAvailableTester implements Runnable{
		private Proxy proxy;

		public ProxyAvailableTester(Proxy proxy) {
			super();
			this.proxy = proxy;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
		/*	int i =0;
			if(i==0)
				return ;*/
			//now ignore socket and vpn
			if(proxy.getType()== null)
				return;
			if(ProxyUtil.validateProxyAvailable(proxy)){
				if(proxy.getAvailblelevel() <0){
					proxy.setAvailblelevel(1);
				}else{
					proxy.setAvailblelevel(proxy.getAvailblelevel() +1);
				}
			}else{
				if(proxy.getAvailblelevel() >0){
					proxy.setAvailblelevel(proxy.getAvailblelevel() -2);
				}else{
					proxy.setAvailblelevel(-1);
				}
			}
			Proxy updateProxy = new Proxy();
			updateProxy.setAvailblelevel(proxy.getAvailblelevel());
			updateProxy.setId(proxy.getId());
			updateProxy.setLastavaibleupdate(new Date());
			proxyService.updateByPrimaryKeySelective(updateProxy);
		}
	}
}
