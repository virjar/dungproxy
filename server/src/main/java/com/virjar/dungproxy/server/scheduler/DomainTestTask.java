package com.virjar.dungproxy.server.scheduler;

import java.util.Date;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.*;

import javax.annotation.Resource;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.client.util.CommonUtil;
import com.virjar.dungproxy.server.core.beanmapper.BeanMapper;
import com.virjar.dungproxy.server.entity.Proxy;
import com.virjar.dungproxy.server.model.DomainIpModel;
import com.virjar.dungproxy.server.model.DomainMetaModel;
import com.virjar.dungproxy.server.model.ProxyModel;
import com.virjar.dungproxy.server.repository.ProxyRepository;
import com.virjar.dungproxy.server.service.DomainIpService;
import com.virjar.dungproxy.server.service.DomainMetaService;
import com.virjar.dungproxy.server.utils.NameThreadFactory;
import com.virjar.dungproxy.server.utils.ProxyUtil;
import com.virjar.dungproxy.server.utils.SysConfig;

/**
 * Created by virjar on 16/9/16.<br/>
 * 针对于指定网站,探测可用资源,探测维度为域名维度,
 */
@Component
public class DomainTestTask implements Runnable, InitializingBean {

    private PriorityQueue<UrlCheckTaskHolder> domainTaskQueue = new PriorityQueue<>();

    private static final Logger logger = LoggerFactory.getLogger(DomainTestTask.class);

    private boolean isRunning = false;

    private ThreadPoolExecutor pool = null;

    private static DomainTestTask instance = null;

    private Set<String> runningDomains = Sets.newConcurrentHashSet();

    @Resource
    private ProxyRepository proxyRepository;

    @Resource
    private DomainIpService domainIpService;

    @Resource
    private DomainMetaService domainMetaService;

    @Resource
    private BeanMapper beanMapper;

    private void init() {
        instance = this;
        isRunning = SysConfig.getInstance().getDomainCheckThread() > 0;
        if (!isRunning) {
            logger.info("domain check task is not running");
            return;
        }
        pool = new ThreadPoolExecutor(SysConfig.getInstance().getDomainCheckThread(), Integer.MAX_VALUE, 30000L,
                TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new NameThreadFactory("domain-check"),
                new ThreadPoolExecutor.CallerRunsPolicy());
        new Thread(this).start();
        new OnlyUrlTestTask().start();
        new OnlyUrlTestTask().start();
    }

    private class OnlyUrlTestTask extends Thread {
        public OnlyUrlTestTask() {
            super("OnlyUrlTestTask");
        }

        @Override
        public void run() {
            while (isRunning) {
                UrlCheckTaskHolder poll = domainTaskQueue.poll();
                if (poll == null) {
                    CommonUtil.sleep(3000);
                    continue;
                }

                if (poll.url == null) {
                    logger.info("get a domain task:{}", poll.domain);
                    domainTaskQueue.offer(poll);
                    CommonUtil.sleep(1000);
                    continue;
                }
                logger.info("get a url test task:{}", poll.url);
                new DomainTester(poll.url).call();
            }
        }
    }

    public static boolean sendDomainTask(final String url) {
        if (instance == null) {
            return false;
        }
        if (!instance.isRunning) {
            logger.info("域名校验组件没有开启,将会把这个任务转发到其他服务器进行处理");
            new Thread() {
                @Override
                public void run() {
                    String s = HttpInvoker.get(SysConfig.getInstance().get("system.domain.test.forward.url"),
                            Lists.<NameValuePair> newArrayList(new BasicNameValuePair("url", url)));
                    logger.info("domain test forward response is {}", s);
                }
            }.start();
            return true;
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
        List<Future<Object>> futureList = Lists.newArrayList();
        while (isRunning) {
            UrlCheckTaskHolder holder = domainTaskQueue.poll();
            if (holder == null) {
                if (pool.getActiveCount() < pool.getCorePoolSize()) {// 在线程池空闲的时候才加入新任务
                    CommonUtil.waitAllFutures(futureList);
                    futureList.clear();
                    genHistoryTestTask();
                }
                CommonUtil.sleep(5000);
                continue;
            }
            if (holder.url == null) {
                futureList.add(pool.submit(new HistoryUrlTester(holder.domain)));
            } else {
                futureList.add(pool.submit(new DomainTester(holder.url)));
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
        public String toString() {
            return "UrlCheckTaskHolder{" + "domain='" + domain + '\'' + ", url='" + url + '\'' + '}';
        }

        @Override
        public int compareTo(UrlCheckTaskHolder o) {
            return priority - o.priority;
        }
    }

    /**
     * 检测曾经检测过的资源,
     */
    private class HistoryUrlTester implements Callable<Object> {

        private String domain;

        public HistoryUrlTester(String domain) {
            this.domain = domain;
        }

        @Override
        public Object call() throws Exception {
            try {
                logger.info("domain checker {} is running", domain);
                DomainIpModel queryDomainIpModel = new DomainIpModel();
                queryDomainIpModel.setDomain(domain);
                int total = domainIpService.selectCount(queryDomainIpModel);
                int pageSize = 100;
                int totalPage = (total + pageSize - 1) / pageSize;// 计算分页场景下页码总数需要这样算,这叫0舍1入。不舍1入?(考虑四舍五入怎么实现)
                for (int nowPage = 0; nowPage < totalPage; nowPage++) {
                    List<DomainIpModel> domainIpModels = domainIpService.selectPage(queryDomainIpModel,
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
                        domainIpService.updateByPrimaryKeySelective(domainIp);// 只更新关心的数据,防止并发环境下的各种同步问题,不是数据库同步,而是逻辑层
                    }
                }
                return null;
            } catch (Exception e) {
                logger.error("error when check domain:{}", domain, e);
                throw e;
            }
        }
    }

    /**
     * 这个任务一个都可能执行几个小时,慢慢等,有耐心,不着急
     */
    private class DomainTester implements Callable<Object> {

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
}
