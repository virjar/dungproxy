package com.virjar.scheduler;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.entity.Proxy;
import com.virjar.model.DomainIpModel;
import com.virjar.model.ProxyModel;
import com.virjar.repository.ProxyRepository;
import com.virjar.service.DomainIpService;
import com.virjar.utils.CommonUtil;
import com.virjar.utils.ProxyUtil;
import com.virjar.utils.SysConfig;

/**
 * Created by virjar on 16/9/16.<br/>
 * 针对于指定网站,探测可用资源,探测维度为域名维度,
 */
@Component
public class DomainTestTask implements Runnable, InitializingBean {
    private LinkedBlockingDeque<String> domainTaskQueue = new LinkedBlockingDeque<>();

    private static final Logger logger = LoggerFactory.getLogger(DomainTestTask.class);

    private boolean isRunning = false;
    private ExecutorService pool = new ThreadPoolExecutor(2, SysConfig.getInstance().getDomainCheckThread(), 30000L,
            TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.CallerRunsPolicy());

    private static DomainTestTask instance = null;
    @Resource
    private ProxyRepository proxyRepository;

    @Resource
    private DomainIpService domainIpService;

    @Resource
    private BeanMapper beanMapper;

    private Set<String> runningDomains = Sets.newConcurrentHashSet();

    public static boolean sendDomainTask(String url) {
        return instance != null && instance.addUrlTask(url);
    }

    public boolean addUrlTask(String url) {
        String domain = CommonUtil.extractDomain(url);
        if (domain == null) {
            return false;
        }
        /**
         * 下面逻辑根据domain去重
         */
        if (runningDomains.contains(domain)) {
            return true;
        } else {
            synchronized (domain.intern()) {
                if (runningDomains.contains(domain)) {
                    return true;
                }
                runningDomains.add(domain);
            }
        }
        return domainTaskQueue.offer(url);
    }

    @Override
    public void run() {
        isRunning = SysConfig.getInstance().isDomainCheckEnable();
        while (isRunning) {
            try {
                String url = domainTaskQueue.take();
                pool.submit(new DomainTester(url));
            } catch (InterruptedException e) {
                logger.error("error when get domain test task", e);
            }
        }
    }

    /**
     * 这个任务一个都可能执行几个小时,慢慢等,有耐心,不着急
     */
    private class DomainTester implements Callable {

        String url;

        DomainTester(String url) {
            this.url = url;
        }

        @Override
        public Object call() {
            try {
                List<Proxy> available = proxyRepository.findAvailable();// 系统可用IP,根据权值排序
                logger.info("domain check total:{} url:{}", available.size(), url);
                for (Proxy proxy : available) {
                    if (ProxyUtil.checkUrl(beanMapper.map(proxy, ProxyModel.class), url)) {
                        DomainIpModel domainIpModel = new DomainIpModel();
                        domainIpModel.setIp(proxy.getIp());
                        domainIpModel.setPort(proxy.getPort());
                        domainIpModel.setDomain(CommonUtil.extractDomain(url));
                        domainIpModel.setProxyId(proxy.getId());
                        domainIpService.create(domainIpModel);
                    }
                }
                logger.info("check end");
                return null;
            }catch ( Exception e){
                logger.info("domain check error",e);
            }
            return null;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(this).start();
        instance = this;
    }

    public static void main(String[] args) {
        DomainTestTask domainTestTask = new DomainTestTask();
        System.out.println(CommonUtil.extractDomain("http://www.scumall.com"));
        System.out.println(CommonUtil.extractDomain("www.baidu.com"));
        System.out.println(CommonUtil.extractDomain("http://git.oschina.net/virjar/proxyipcenter/commits/master"));
    }
}
