package com.virjar.ipproxy.util;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by virjar on 16/9/16.
 */
public class CommonUtil {
    private static Pattern domainPattern = Pattern.compile("://([^/]+)");
    private static Pattern ipPattern = Pattern.compile(
            "^([1-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])(\\.([0-9]|[1-9][0-9]|1[0-9][0-9]|2[0-4][0-9]|25[0-5])){3}$");

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
}
