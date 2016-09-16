package com.virjar.utils;

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
}
