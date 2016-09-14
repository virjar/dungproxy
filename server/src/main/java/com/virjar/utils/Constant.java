package com.virjar.utils;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

/**
 * Created by virjar on 16/9/14.
 */
public class Constant {
    public static final String HEADER_CHECK_HEADER = "ProxyHeaderCheck";
    public static final String HEADER_CHECK_VALUE = "weijia.deng";
    public static final Header CHECK_HEADER = new BasicHeader(HEADER_CHECK_HEADER, HEADER_CHECK_VALUE);
}
