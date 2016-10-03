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

    /**
     * 迁移多少天以外的数据 默认是3 包括三天
     */
    public static final int STEP = 10;
    /**
     * 迁移connection_score 小于等于 多少的数据 默认是 -7
     */
    public static final int THRESHOLD = -12;
}
