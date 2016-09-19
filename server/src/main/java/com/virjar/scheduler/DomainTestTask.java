package com.virjar.scheduler;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
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
    private ExecutorService pool = new ThreadPoolExecutor(SysConfig.getInstance().getDomainCheckThread(),
            SysConfig.getInstance().getDomainCheckThread(), 30000L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<Runnable>(), Executors.defaultThreadFactory(),
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
        try {
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
        } catch (Exception e) {
            logger.error("为啥任务仍不进来?", e);
            return false;
        }
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
                    logger.info("url:{} domain check :{}", url, JSONObject.toJSONString(proxy));
                    if (ProxyUtil.checkUrl(beanMapper.map(proxy, ProxyModel.class), url)) {
                        DomainIpModel domainIpModel = new DomainIpModel();
                        domainIpModel.setIp(proxy.getIp());
                        domainIpModel.setPort(proxy.getPort());
                        domainIpModel.setDomain(CommonUtil.extractDomain(url));
                        domainIpModel.setProxyId(proxy.getId());
                        domainIpModel.setTestUrl(url);
                        domainIpModel.setDomainScoreDate(new Date());
                        domainIpService.create(domainIpModel);
                    }
                }
                logger.info("check end");
                return null;
            } catch (Throwable e) {
                e.printStackTrace();
                logger.info("domain check error", e);
            } finally {// 结束后清除锁,允许任务再次触发
                runningDomains.remove(CommonUtil.extractDomain(url));
            }
            return null;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        new Thread(this, "domain_test_task").start();
        instance = this;
    }

    public static void main(String[] args) {
        DomainTestTask domainTestTask = new DomainTestTask();
        /*
         * System.out.println(CommonUtil.extractDomain("http://www.scumall.com"));
         * System.out.println(CommonUtil.extractDomain("www.baidu.com"));
         * System.out.println(CommonUtil.extractDomain("http://git.oschina.net/virjar/proxyipcenter/commits/master"));
         */
        domainTestTask.addUrlTask("http://www.scumall.com");
        domainTestTask.addUrlTask("http://git.oschina.net/virjar/proxyipcenter/");
        domainTestTask.addUrlTask("http://www.cnblogs.com/linjiqin/p/3214725.html");
        domainTestTask.addUrlTask("http://blog.sina.com.cn/s/blog_7768d2210101ajj6.html");
        domainTestTask.addUrlTask("http://china.ynet.com/3.1/1609/17/11742693.html");
        domainTestTask.addUrlTask(
                "https://www.aliyun.com/product/ddos/?utm_medium=images&utm_source=criteo&utm_campaign=lf&utm_content=se_276183");
        domainTestTask.addUrlTask("http://cn.ynet.com/3.1/1609/17/11742182.html");
        domainTestTask.addUrlTask("http://news.163.com/photoview/00AP0001/2198365.html#p=C14T5IU900AP0001");
        domainTestTask.addUrlTask("http://www.bobo.com/660000?f=163.picRec");
        domainTestTask.addUrlTask("http://china.ynet.com/3.1/1609/16/11739239.html");
        domainTestTask.addUrlTask("http://war.163.com/16/0915/08/C109SQQC000181KT.html");
        domainTestTask.addUrlTask("http://bbs.tiexue.net/post2_11429708_1.html");
        domainTestTask.addUrlTask("http://fun.youth.cn/yl24xs/201609/t20160916_8662334_1.htm");
        domainTestTask.addUrlTask("http://www.chinadaily.com.cn/interface/yidian/1138561/2016-09-17/cd_26809500.html");
        domainTestTask.addUrlTask("http://china.ynet.com/3.1/1609/16/11741080.html");
        domainTestTask.addUrlTask("http://china.ynet.com/3.1/1609/16/11741050.html");
        domainTestTask.addUrlTask("http://news.wmxa.cn/n/201609/373635_2.html");

    }
}
