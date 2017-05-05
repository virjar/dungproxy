package com.virjar.dungproxy.client.model;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.Header;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.virjar.dungproxy.client.ippool.DomainPool;
import com.virjar.dungproxy.client.ippool.IpPool;
import com.virjar.dungproxy.client.ippool.config.DomainContext;
import com.virjar.dungproxy.client.ippool.config.DungProxyContext;
import com.virjar.dungproxy.client.ippool.strategy.Offline;
import com.virjar.dungproxy.client.ippool.strategy.Scoring;

/**
 * Description: AvProxy
 *
 * @author lingtong.fu
 * @version 2016-09-11 09:52
 */
public class AvProxy {

    private static AtomicLong proxyNumberChange = new AtomicLong(0);

    // IP地址
    private String ip;

    // 端口号
    private Integer port;

    // 用户名,如果本IP需要登录认证
    private String username;

    // 密码,如果本IP需要登录认证
    private String password;

    // 有些代理是通过请求头进行认证的
    private List<Header> authenticationHeaders = Lists.newArrayList();

    private boolean isInit = true;

    // 最后被引用的时间
    private long lastUsedTime = 0;

    private DomainPool domainPool;

    private boolean disable = false;

    // 引用次数,当引用次数为0的时候,由调度任务清除该
    private AtomicInteger referCount = new AtomicInteger(0);

    private AtomicInteger failedCount = new AtomicInteger(0);

    // 平均打分 需要归一化,在0-1的一个小数
    private double avgScore = 0;

    // 是否被分发出去
    private boolean referFlag = false;

    // TODO 移到context里面
    public static boolean needRecordChange = true;

    private DungProxyContext dungProxyContext;

    private Scoring scoring;

    private Offline offline;

    private DomainContext domainContext;

    private long resueTime;

    public AvProxy(DomainContext domainContext) {
        this.domainContext = domainContext;
        this.dungProxyContext = domainContext.getDungProxyContext();
        this.scoring = domainContext.getScoring();
        this.offline = domainContext.getOffline();
        if (needRecordChange) {
            recordProxyChange();
        }
    }

    public void recordProxyChange() {
        if (proxyNumberChange.incrementAndGet() % 10 == 0) {// 每当有10个IP加入或者下线,则进行一次序列化
            if (IpPool.getInstance() == null) {
                return;// 说明是初始化的时候,在递归调用到这里了。放弃序列化
            }
            dungProxyContext.getAvProxyDumper().serializeProxy(Maps.transformValues(IpPool.getInstance().getPoolInfo(),
                    new Function<List<AvProxy>, List<AvProxyVO>>() {
                        @Override
                        public List<AvProxyVO> apply(List<AvProxy> input) {
                            return Lists.transform(input, new Function<AvProxy, AvProxyVO>() {
                                @Override
                                public AvProxyVO apply(AvProxy input) {
                                    return AvProxyVO.fromModel(input);
                                }
                            });
                        }
                    }));
        }
    }

    // 虽然加锁,但是锁等待概率很小
    public synchronized void reset() {
        referCount.set(0);
        failedCount.set(0);
        disable = false;
    }

    public void recordFailed() {
        if (!referFlag) {// 重复记录失败使用了
            return;
        }
        referFlag = false;
        avgScore = scoring.newAvgScore(this, domainContext.getScoreFactory(), false);
        failedCount.incrementAndGet();
        if (offline.needOffline(this)) {
            offline();// 资源下线,下次将不会分配这个IP了
        } else {
            domainPool.adjustPriority(this);
        }
    }

    public void adjustPriority() {
        domainPool.adjustPriority(this);
    }

    /**
     * 两次使用之间,如果没有发生过异常,那么认为使用成功了
     */
    public void recordUsage() {
        if (referFlag) {// 被使用过,但是没有失败报告,认为上次使用成功,不考虑段时间并发问题导致的反馈不及时问题,那种场景会导致多记录一次成功
            avgScore = scoring.newAvgScore(this, domainContext.getScoreFactory(), true);
        }
        referFlag = true;
        lastUsedTime = System.currentTimeMillis();
        referCount.incrementAndGet();
    }

    /**
     * 封禁IP一定时间,比如封禁1个小时,那么IP不会下线,但是一个小时之后才能被使用
     * 
     * @param blockTimeStamp 时间戳,单位毫秒
     */
    public void block(long blockTimeStamp) {
        domainPool.block(this, blockTimeStamp);
    }

    public void offline(boolean force) {
        if (!force) {
            return;// 只有force的时候才真正下线
        }
        if (needRecordChange) {
            recordProxyChange();
        }
        disable = true;
        domainPool.offline(this);
    }

    public void offline() {
        offline(true);
    }

    public long getLastUsedTime() {
        return lastUsedTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        AvProxy avProxy = (AvProxy) o;

        return ip != null ? ip.equals(avProxy.ip)
                : avProxy.ip == null && (port != null ? port.equals(avProxy.port) : avProxy.port == null);

    }

    @Override
    public int hashCode() {
        int result = ip != null ? ip.hashCode() : 0;
        result = 31 * result + (port != null ? port.hashCode() : 0);
        return result;
    }

    public boolean isDisable() {
        return disable;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public boolean isInit() {
        return isInit;
    }

    public void setInit(boolean init) {
        isInit = init;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<Header> getAuthenticationHeaders() {
        return authenticationHeaders;
    }

    public void setAuthenticationHeaders(List<Header> authenticationHeaders) {
        this.authenticationHeaders = authenticationHeaders;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public DomainPool getDomainPool() {
        return domainPool;
    }

    public void setDomainPool(DomainPool domainPool) {
        this.domainPool = domainPool;
    }

    @Deprecated
    public AvProxy copy() {
        AvProxy newProxy = new AvProxy(domainContext);
        newProxy.domainPool = domainPool;
        newProxy.disable = disable;
        newProxy.ip = ip;
        newProxy.isInit = isInit;
        newProxy.port = port;
        newProxy.failedCount = new AtomicInteger(failedCount.get());
        newProxy.referCount = new AtomicInteger(referCount.get());
        newProxy.avgScore = avgScore;
        // newProxy.score = new Score(score.getAvgScore(), new AtomicInteger(score.getFailedCount().get()),
        // new AtomicInteger(score.getReferCount().get()));
        return newProxy;
    }

    public Integer getFailedCount() {
        return failedCount.get();
    }

    public Integer getReferCount() {
        return referCount.get();
    }

    public double getAvgScore() {
        return avgScore;
    }

    public void setAvgScore(double avgScore) {
        this.avgScore = avgScore;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = new AtomicInteger(failedCount);
    }

    public void setReferCount(Integer referCount) {
        this.referCount = new AtomicInteger(referCount);
    }

    public long getResueTime() {
        return resueTime;
    }

    public void setResueTime(long resueTime) {
        this.resueTime = resueTime;
    }
}
