package com.virjar.dungproxy.client.util;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * Created by virjar on 16/9/16.
 */
public class CommonUtil {
    private static Pattern domainPattern = Pattern.compile("://([^/]+)");
    private static Pattern ipPattern = Pattern.compile(
            "^([1-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3}$");

    private static Pattern ipAndPortPattern = Pattern.compile(
            "([1-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3}:\\d{1,6}");
    private static final Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    public static String extractDomain(String url) {
        if (StringUtils.isEmpty(url)) {
            return null;
        }
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }
        Matcher matcher = domainPattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static void sleep(long timestamp) {
        try {
            Thread.sleep(timestamp);
        } catch (InterruptedException e) {
            logger.error("thread sleep error", e);
        }
    }

    public static <T> void waitAllFutures(List<Future<T>> futureList) {
        for (Future<T> future : futureList) {
            try {
                future.get();
            } catch (Exception e) {
                logger.error("error when wait future task", e);
            }
        }
    }

    public static String ensurePathExist(String fileName) throws IOException {
        File parentFile = new File(fileName).getParentFile();
        if (parentFile.exists() && parentFile.isFile()) {
            throw new IOException(
                    "can not create directory for file:" + fileName + " it already exist ,and it`s a file");
        }
        if (parentFile.exists() && parentFile.isDirectory()) {
            return fileName;
        }
        if (!parentFile.mkdirs()) {
            throw new IOException("can not create directory for file:" + fileName);
        }
        return fileName;
    }

    public static boolean isIPAddress(String ipAddress) {
        if (StringUtils.isEmpty(ipAddress)) {
            return false;
        }

        Matcher matcher = ipPattern.matcher(ipAddress);
        return matcher.find();
    }

    public static boolean isPlainProxyItem(String ipAndPort) {
        return ipAndPortPattern.matcher(ipAndPort).find();
    }

    public static String toString(java.util.Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }

    private static Date startTime;

    public final static Date getStartTime() {
        if (startTime == null) {
            startTime = new Date(ManagementFactory.getRuntimeMXBean().getStartTime());
        }
        return startTime;
    }

    private static final List<Header> defaultHeaders = Lists.newArrayList();
    static {
        defaultHeaders.add(new BasicHeader("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"));
        // 这个默认会添加,值为 gzip, deflate
        // defaultHeaders.add(new BasicHeader("Accept-Encoding", "gzip, deflate, sdch, br"));
        defaultHeaders.add(new BasicHeader("Accept-Language", "en-US,en;q=0.8"));
        defaultHeaders.add(new BasicHeader("Cache-Control", "max-age=0"));
    }

    public static List<Header> defaultHeader() {
        return defaultHeaders;
    }
}
