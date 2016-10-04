package com.virjar.crawler;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.alibaba.fastjson.JSONObject;
import com.virjar.crawler.extractor.XmlModeFetcher;
import com.virjar.entity.Proxy;
import com.virjar.ipproxy.httpclient.HttpInvoker;
import com.virjar.ipproxy.util.PoolUtil;

public class Collector {

    private int batchsize;

    private URLGenerator urlGenerator;

    private XmlModeFetcher fetcher;

    private String charset = "utf-8";

    private boolean hasdetectcharset = false;

    private String website = "";

    private long lastactivity = 0;

    private long hibrate = 20000;

    private Logger logger = Logger.getLogger(Collector.class);

    private long getnumber = 0;

    private String errorinfo = "";

    private int failedTimes = 0;

    private int sucessTimes = 0;

    private HttpClientContext httpClientContext = HttpClientContext.adapt(new BasicHttpContext());

    private String lastUrl = "";

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

    public long getGetnumber() {
        return getnumber;
    }

    public void setGetnumber(long getnumber) {
        this.getnumber = getnumber;
    }

    public List<Proxy> newProxy() {
        int num = 0;
        int failedtimes = 0;
        String url = "";
        List<Proxy> ret = new ArrayList<Proxy>();

        if (System.currentTimeMillis() - lastactivity < hibrate) {
            return ret;
        }
        lastactivity = System.currentTimeMillis();
        url = urlGenerator.newURL();
        while (num < batchsize) {
            try {
                lastUrl = url;
                PoolUtil.cleanProxy(httpClientContext);// 每次使用不用的代理IP
                String response = HttpInvoker.get(url, httpClientContext);
                if (StringUtils.isEmpty(response)) {
                    PoolUtil.recordFailed(httpClientContext);
                }
                if (StringUtils.isNotEmpty(response)) {
                    List<String> fetchresult = fetcher.fetch(response);
                    // 异常会自动记录代理IP使用失败,这里的情况是请求成功,但是网页可能不是正确的,当然这个反馈可能不是完全准确的,不过也无所谓,本身IP下线是在失败率达到一定的量的情况
                    if (fetchresult.size() == 0) {
                        // PoolUtil.recordFailed(httpClientContext);
                        failedtimes++;
                        if (failedtimes > 3) {
                            return ret;
                        }
                    } else {
                        failedtimes = 0;
                        sucessTimes++;
                    }
                    for (String str : fetchresult) {
                        num++;
                        Proxy parseObject = null;
                        try {
                            parseObject = JSONObject.parseObject(str, Proxy.class);
                        } catch (Exception e) {
                            e.printStackTrace();
                            num--;
                            continue;
                        }
                        parseObject.setAvailbelScore(0L);
                        parseObject.setConnectionScore(0L);
                        parseObject.setSource(url);
                        ret.add(parseObject);
                    }
                    if (fetchresult.size() == 0) {
                        urlGenerator.reset();
                    }
                    Thread.sleep(2000);
                    url = urlGenerator.newURL();
                }
            } catch (Exception e) {// 发生socket异常不切换url
                if (!(e instanceof SocketTimeoutException) && !(e instanceof SocketException)) {
                    PoolUtil.recordFailed(httpClientContext);
                }
                errorinfo = url + ":" + e;
                failedTimes++;
                failedtimes++;
            }

        }
        getnumber += ret.size();
        if (ret.size() == 0) {
            logger.info("empty resource for :" + url);
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
                        Collector collecter = new Collector();
                        collecter.website = next.elementText("website");
                        String hierate = next.elementText("hierate");
                        collecter.hibrate = parseTime(hierate);

                        String usecharset = next.elementText("charset");
                        if (usecharset != null) {
                            collecter.charset = usecharset;
                        }
                        String bachSize = next.elementText("batchsize");
                        collecter.batchsize = 30;
                        if (bachSize != null) {
                            try {
                                collecter.batchsize = Integer.parseInt(bachSize);
                            } catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }

                        collecter.fetcher = new XmlModeFetcher(
                                IOUtils.toString(Collector.class.getResourceAsStream(next.elementText("fetcher"))));
                        Element classgenerator = next.element("classgenerator");
                        boolean hasagenerator = false;
                        if (classgenerator != null && !"".equals(classgenerator.getTextTrim())) {
                            Class<? extends URLGenerator> clazz = (Class<? extends URLGenerator>) Collector.class
                                    .getClassLoader().loadClass(classgenerator.getTextTrim());
                            Constructor<? extends URLGenerator> constructor = clazz.getConstructor();
                            collecter.urlGenerator = (URLGenerator) constructor.newInstance();
                            hasagenerator = true;
                        }
                        Element wildcardgenerator = next.element("wildcardgenerator");
                        if (!hasagenerator && wildcardgenerator != null
                                && !"".equals(wildcardgenerator.getTextTrim())) {
                            collecter.urlGenerator = new WildCardURLGenerator(wildcardgenerator.getTextTrim());
                            hasagenerator = true;
                        }
                        if (hasagenerator) {
                            ret.add(collecter);
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
