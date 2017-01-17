package com.virjar.dungproxy.client.samples.poolstrategy;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.math.NumberUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.google.common.io.LineProcessor;
import com.virjar.dungproxy.client.ippool.strategy.ResourceFacade;
import com.virjar.dungproxy.client.model.AvProxyVO;
import com.virjar.dungproxy.client.util.CommonUtil;

/**
 * Created by virjar on 17/1/17.<br/>
 * 自定义数据源,传统的基于文件的加载方式,注意替换 filePath为真实文件路径,文件格式如下
 * 
 * <pre>
 *     ip:port
 *     ip2:port
 *     123.34.65.23:80
 *     34.45.65.34:8123
 * </pre>
 */
public class CustomIPSource implements ResourceFacade {
    private static final Logger logger = LoggerFactory.getLogger(CustomIPSource.class);
    private Map<String, List<String>> cache = Maps.newConcurrentMap();
    private Set<String> allProxies = Sets.newConcurrentHashSet();

    private static final String filePath = "";// 通过文件加载IP,这里替换为文件路径

    private static final Splitter ipAndPortSpitter = Splitter.on(":");

    @Override
    public List<AvProxyVO> importProxy(String domain, String testUrl, Integer number) {
        if (number == null || number == 0) {
            number = 40;// 拍脑门儿随意写的
        }
        // 获取缓存数据
        List<String> ipList = cache.get(domain);
        if (ipList == null) {
            synchronized (this) {
                ipList = cache.get(domain);
                if (ipList == null) {
                    ipList = createNewDomainCache();
                    cache.put(domain, ipList);
                }
            }
        }
        // IP不足量,增量加载IP
        if (ipList.size() < number) {
            importAllData();
        }

        // 从缓存总拿出对应数目的IP,并删除缓存
        Iterator<String> iterator = ipList.iterator();
        List<AvProxyVO> ret = Lists.newArrayList();
        while (iterator.hasNext()) {
            String next = iterator.next();
            List<String> strings = ipAndPortSpitter.splitToList(next);
            AvProxyVO avProxyVO = new AvProxyVO();
            avProxyVO.setIp(strings.get(0));
            avProxyVO.setPort(NumberUtils.toInt(strings.get(1)));
            ret.add(avProxyVO);
            iterator.remove();
            if (ret.size() >= number) {
                return ret;
            }
        }

        return ret;
    }

    private List<String> createNewDomainCache() {
        if (allProxies.size() == 0) {
            importAllData();
        }
        return Lists.newArrayList(allProxies);
    }

    @Override
    public void feedBack(String domain, List<AvProxyVO> avProxies, List<AvProxyVO> disableProxies) {
        // 看自己的需求是否需要反馈IP使用情况到自己的IP数据源了
    }

    private void extendCache(List<String> newData) {
        for (List<String> domainItem : cache.values()) {
            domainItem.addAll(newData);
        }
    }

    @Override
    public List<AvProxyVO> allAvailable() {
        if (allProxies.size() == 0) {
            importAllData();
        }
        return Lists.transform(Lists.newArrayList(allProxies), new Function<String, AvProxyVO>() {
            @Override
            public AvProxyVO apply(String input) {
                List<String> strings = ipAndPortSpitter.splitToList(input);
                AvProxyVO avProxyVO = new AvProxyVO();
                avProxyVO.setIp(strings.get(0));
                avProxyVO.setPort(NumberUtils.toInt(strings.get(1)));
                return avProxyVO;
            }
        });
    }

    private synchronized void importAllData() {
        try {
            List<String> avProxyVOs = Files.readLines(new File(filePath), Charset.defaultCharset(),
                    new LineProcessor<List<String>>() {
                        private List<String> ret = Lists.newArrayList();

                        @Override
                        public boolean processLine(String line) throws IOException {
                            if (CommonUtil.isPlainProxyItem(line)) {
                                return true;
                            }
                            logger.warn("{}不是合法的IP条目", line);
                            return false;
                        }

                        @Override
                        public List<String> getResult() {
                            return ret;
                        }
                    });
            allProxies.addAll(avProxyVOs);
            extendCache(avProxyVOs);
            logger.info("本次共加载:{}条IP数据", avProxyVOs.size());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
