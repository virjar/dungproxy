package com.virjar.scheduler;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.*;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Sets;
import com.virjar.core.beanmapper.BeanMapper;
import com.virjar.entity.Proxy;
import com.virjar.model.DomainIpModel;
import com.virjar.model.DomainMetaModel;
import com.virjar.model.ProxyModel;
import com.virjar.repository.ProxyRepository;
import com.virjar.service.DomainIpService;
import com.virjar.service.DomainMetaService;
import com.virjar.utils.*;

/**
 * Created by virjar on 16/9/16.<br/>
 * 针对于指定网站,探测可用资源,探测维度为域名维度,
 */
@Component
public class DomainTestTask implements Runnable, InitializingBean {
    private PriorityQueue<UrlCheckTaskHolder> domainTaskQueue = new PriorityQueue<>();

    private static final Logger logger = LoggerFactory.getLogger(DomainTestTask.class);

    private boolean isRunning = false;
    private ExecutorService pool = null;

    private static DomainTestTask instance = null;
    @Resource
    private ProxyRepository proxyRepository;

    @Resource
    private DomainIpService domainIpService;

    @Resource
    private DomainMetaService domainMetaService;

    @Resource
    private BeanMapper beanMapper;

    private Set<String> runningDomains = Sets.newConcurrentHashSet();

    private void init() {
        isRunning = SysConfig.getInstance().getDomainCheckThread() > 0;
        if (!isRunning) {
            logger.info("domain check task is not running");
            return;
        }
        pool = new ThreadPoolExecutor(SysConfig.getInstance().getDomainCheckThread(),
                SysConfig.getInstance().getDomainCheckThread(), 30000L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(), new NameThreadFactory("domain-check"),
                new ThreadPoolExecutor.CallerRunsPolicy());
        instance = this;
        new Thread(this).start();
    }

    public static boolean sendDomainTask(String url) {
        if (instance == null) {
            return false;
        }
        if (!instance.isRunning) {
            try {
                BrowserHttpClientPool.getInstance().borrow()
                        .get(String.format(SysConfig.getInstance().get("system.domain.test.forward.url"), url));
                return true;// TODO
            } catch (IOException e) {
                logger.error("error when forward domain test task to sub server");
                return false;
            }
        }
        return instance.addUrlTask(url);
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
            UrlCheckTaskHolder urlCheckTaskHolder = new UrlCheckTaskHolder();
            urlCheckTaskHolder.priority = 10;
            urlCheckTaskHolder.url = url;
            urlCheckTaskHolder.domain = domain;
            return domainTaskQueue.offer(urlCheckTaskHolder);
        } catch (Exception e) {
            logger.error("为啥任务仍不进来?", e);
            return false;
        }
    }

    @Override
    public void run() {
        while (isRunning) {
            UrlCheckTaskHolder holder = domainTaskQueue.poll();
            if (holder == null) {
                genHistoryTestTask();
                CommonUtil.sleep(5000);
                continue;
            }
            if (holder.url == null) {
                pool.submit(new HistoryUrlTester(holder.domain));
            } else {
                pool.submit(new DomainTester(holder.url));
            }
        }
    }

    private void genHistoryTestTask() {
        List<DomainMetaModel> domainMetaModels = domainMetaService.selectPage(null, null);
        for (DomainMetaModel domainMetaModel : domainMetaModels) {
            UrlCheckTaskHolder urlCheckTaskHolder = new UrlCheckTaskHolder();
            urlCheckTaskHolder.priority = 1;
            urlCheckTaskHolder.domain = domainMetaModel.getDomain();
            domainTaskQueue.offer(urlCheckTaskHolder);
        }
    }

    private class UrlCheckTaskHolder implements Comparable<UrlCheckTaskHolder> {
        int priority;
        String url;
        String domain;

        @Override
        public int compareTo(UrlCheckTaskHolder o) {
            return priority - o.priority;
        }
    }

    /**
     * 检测曾经检测过的资源,
     */
    private class HistoryUrlTester implements Callable {

        private String domain;

        public HistoryUrlTester(String domain) {
            this.domain = domain;
        }

        @Override
        public Object call() throws Exception {
            DomainIpModel domainIpModel = new DomainIpModel();
            domainIpModel.setDomain(domain);
            int total = domainIpService.selectCount(domainIpModel);
            int pageSize = 100;
            int totalPage = (total + pageSize - 1) / pageSize;// 计算分页场景下页码总数需要这样算,这叫0舍1入。不舍1入?(考虑四舍五入怎么实现)
            for (int nowPage = 0; nowPage < totalPage; nowPage++) {
                List<DomainIpModel> domainIpModels = domainIpService.selectPage(domainIpModel,
                        new PageRequest(nowPage, pageSize));
                for (DomainIpModel domainIp : domainIpModels) {

                    if (ProxyUtil.checkUrl(domainIp.getIp(), domainIp.getPort(), domainIp.getTestUrl())) {
                        if (domainIp.getDomainScore() < 0) {
                            domainIp.setDomainScore(1L);
                        } else {
                            domainIp.setDomainScore(domainIp.getDomainScore() + 1);
                        }
                    } else {
                        if (domainIp.getDomainScore() > 0) {// 快速降权
                            domainIp.setDomainScore(domainIp.getDomainScore()
                                    - (long) Math.log((double) domainIp.getDomainScore() + 3));
                        } else {
                            domainIp.setDomainScore(domainIp.getDomainScore() - 1);
                        }
                    }
                    domainIp.setDomain(null);
                    domainIp.setIp(null);
                    domainIp.setPort(null);
                    domainIp.setSpeed(null);
                    domainIp.setCreatetime(null);
                    domainIp.setDomainScoreDate(new Date());
                    domainIpService.updateByPrimaryKeySelective(domainIpModel);// 只更新关心的数据,防止并发环境下的各种同步问题,不是数据库同步,而是逻辑层
                }
            }
            return null;
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
                DomainMetaModel domainMetaModel = new DomainMetaModel();
                domainMetaModel.setDomain(CommonUtil.extractDomain(url));
                if (domainMetaService.selectCount(domainMetaModel) == 0) {
                    domainMetaService.createSelective(domainMetaModel);
                }

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
                logger.info("domain check error", e);
            } finally {// 结束后清除锁,允许任务再次触发
                runningDomains.remove(CommonUtil.extractDomain(url));
            }
            return null;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        init();
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
