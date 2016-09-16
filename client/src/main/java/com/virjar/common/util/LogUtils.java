package com.virjar.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Classname: LogUtils
 * Version: 1.0
 * Date: 16-3-5.
 * Created by lingtong
 */
public class LogUtils {
    private static final Logger log = LoggerFactory.getLogger(LogUtils.class);
    /** trace */
    public static void trace(String traceInfo){
        log.trace(traceInfo);
    }
    /** debug */
    public static void debug(String debugInfo){
        log.debug(debugInfo);
    }
    /** info */
    public static void info(String var1){
        log.info(var1);
    }
    public static void info(String var1, Object var2){
        log.info(var1,var2);
    }
    public static void info(String var1, Object var2, Object var3){
        log.info(var1, var2, var3);
    }
    public static void info(String var1, Object... var2){
        log.info(var1, var2);
    }
    /** warn */
    public static void warn(String warnInfo){
        log.warn(warnInfo);
    }
    /** error */
    public static void error(String errorInfo){
        log.error(errorInfo);
    }
    public static void error(String errorInfo, Object var2){
        log.error(errorInfo, var2);
    }
    public static void error(String errorInfo, Object var2, Object var3){
        log.error(errorInfo, var2, var3);
    }

    public static void error(String errorInfo, Object... var2){
        log.error(errorInfo, var2);
    }
}
