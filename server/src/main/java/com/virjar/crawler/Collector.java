package com.virjar.crawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.BasicHttpContext;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.virjar.crawler.extractor.XmlModeFetcher;
import com.virjar.entity.Proxy;
import com.virjar.ipproxy.httpclient.HttpInvoker;
import com.virjar.ipproxy.ippool.config.ObjectFactory;
import com.virjar.ipproxy.util.CommonUtil;
import com.virjar.ipproxy.util.PoolUtil;

public class Collector {

    private int batchsize;

    private URLGenerator urlGenerator;

    private XmlModeFetcher fetcher;

    private String website = "";

    private long lastactivity = 0;

    private long hibrate = 20000;

    private Logger logger = LoggerFactory.getLogger(Collector.class);

    private long getnumber = 0;

    private String errorinfo = "";

    private int failedTimes = 0;

    private int sucessTimes = 0;

    private HttpClientContext httpClientContext = HttpClientContext.adapt(new BasicHttpContext());

    private String lastUrl = "";

    // 如果存在,拿到key代表数据成功获取,而不是通过是否提取到数据来判定是否成功
    private String sucessKey = null;

    public String getErrorinfo() {
        return errorinfo;
    }

    public int getBatchsize() {
        return batchsize;
    }

    public void setBatchsize(int batchsize) {
        this.batchsize = batchsize;
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

    public long getGetnumber() {
        return getnumber;
    }

    public void setGetnumber(long getnumber) {
        this.getnumber = getnumber;
    }

    public List<Proxy> newProxy() {
        int failedTimes = 0;
        List<Proxy> ret = Lists.newArrayList();

        if (System.currentTimeMillis() - lastactivity < hibrate) {
            return ret;
        }
        logger.info("begin collector:{}", this.website);
        lastactivity = System.currentTimeMillis();
        lastUrl = urlGenerator.newURL();
        while (ret.size() < batchsize) {
            if(failedTimes > 20){//不论如何,连续一定次数失败,重置url生成器
                urlGenerator.reset();
                break;
            }
            try {
                logger.info("request url:{} failedTimes:{}", lastUrl, failedTimes);
                String response = HttpInvoker.get(lastUrl, httpClientContext);
                if (StringUtils.isEmpty(response)) {
                    PoolUtil.recordFailed(httpClientContext);
                    failedTimes++;
                    if (failedTimes > 5) {
                        break;
                    }
                }
                if (StringUtils.isNotEmpty(response)) {
                    List<Proxy> fetchResult = convert(fetcher.fetch(response), lastUrl);
                    // 异常会自动记录代理IP使用失败,这里的情况是请求成功,但是网页可能不是正确的,当然这个反馈可能不是完全准确的,不过也无所谓,本身IP下线是在失败率达到一定的量的情况
                    if (fetchResult.size() == 0) {
                        failedTimes++;
                        if (StringUtils.isEmpty(sucessKey) || !StringUtils.contains(response, sucessKey)) {
                            PoolUtil.recordFailed(httpClientContext);// 这种情况不reset url,属于代理失败
                        } else {
                            if (failedTimes > 5) {
                                urlGenerator.reset();
                                break;
                            }
                        }
                    } else {
                        PoolUtil.cleanProxy(httpClientContext);// 每次使用不用的代理IP
                        failedTimes = 0;
                        sucessTimes++;
                        ret.addAll(fetchResult);
                        CommonUtil.sleep(2000);
                        lastUrl = urlGenerator.newURL();
                    }
                }
            } catch (Exception e) {// 发生socket异常不切换url
                errorinfo = lastUrl + ":" + e;
                failedTimes++;
                if (failedTimes > 5) {
                    break;
                }
            }

        }
        this.failedTimes += failedTimes;
        getnumber += ret.size();
        return ret;
    }

    public List<Proxy> convert(List<String> fetchResult, String source) {
        List<Proxy> ret = Lists.newArrayList();
        for (String str : fetchResult) {
            try {
                Proxy parseObject = JSONObject.parseObject(str, Proxy.class);
                if (!CommonUtil.isIPAddress(parseObject.getIp())) {
                    continue;
                }
                parseObject.setAvailbelScore(0L);
                parseObject.setConnectionScore(0L);
                parseObject.setSource(source);
                ret.add(parseObject);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        return ret;
    }

    public static List<Collector> buildfromSource(String source) throws DocumentException, IOException {
        if (source == null) {
            source = "/handmaper.xml";
        }
        List<Collector> ret = new ArrayList<Collector>();
        Document parseText = DocumentHelper.parseText(IOUtils.toString(Collector.class.getResourceAsStream(source)));

        Element rootElement = parseText.getRootElement();
        if (rootElement != null && "handmapers".equals(rootElement.getName())) {
            Iterator<Element> elementIterator = rootElement.elementIterator();
            while (elementIterator.hasNext()) {
                Element next = elementIterator.next();
                if ("maper".equals(next.getName())) {
                    try {
                        if (!"true".equals(next.element("enable").getTextTrim())) {
                            continue;
                        }
                        Collector collector = new Collector();
                        collector.website = next.elementText("website");
                        String hierate = next.elementText("hierate");
                        collector.hibrate = parseTime(hierate);

                        String bachSize = next.elementText("batchsize");
                        collector.batchsize = 30;
                        if (bachSize != null) {
                            collector.batchsize = NumberUtils.toInt(bachSize, collector.batchsize);
                        }

                        collector.sucessKey = next.elementText("successKey");

                        collector.fetcher = new XmlModeFetcher(
                                IOUtils.toString(Collector.class.getResourceAsStream(next.elementText("fetcher"))));
                        Element classgenerator = next.element("classgenerator");
                        boolean hasagenerator = false;
                        if (classgenerator != null && !"".equals(classgenerator.getTextTrim())) {
                            collector.urlGenerator = ObjectFactory.newInstance(classgenerator.getTextTrim());
                            hasagenerator = true;
                        }
                        Element wildcardgenerator = next.element("wildcardgenerator");
                        if (!hasagenerator && wildcardgenerator != null
                                && !"".equals(wildcardgenerator.getTextTrim())) {
                            collector.urlGenerator = new WildCardURLGenerator(wildcardgenerator.getTextTrim());
                            hasagenerator = true;
                        }
                        if (hasagenerator) {
                            ret.add(collector);
                        }
                    } catch (Exception e) {

                    }
                }
            }
        }
        return ret;
    }

    private static long parseTime(String str) {
        if (str == null)
            return 0;
        String newStr = str.toLowerCase();
        try {
            if (newStr.endsWith("h")) {
                return Long.parseLong(newStr.substring(0, newStr.length() - 1)) * 60 * 60 * 1000;
            } else if (newStr.endsWith("m")) {
                return Long.parseLong(newStr.substring(0, newStr.length() - 1)) * 60 * 1000;
            } else if (newStr.endsWith("s")) {
                return Long.parseLong(newStr.substring(0, newStr.length() - 1)) * 1000;
            } else {
                return Long.parseLong(newStr.substring(0, newStr.length() - 1));
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

    public String getLastUrl() {
        return lastUrl;
    }
}
