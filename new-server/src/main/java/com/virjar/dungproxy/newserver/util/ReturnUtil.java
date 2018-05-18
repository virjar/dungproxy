package com.virjar.dungproxy.newserver.util;

import com.virjar.dungproxy.newserver.model.WebJsonResponse;

/**
 * Created by virjar on 2018/5/18.
 */
public class ReturnUtil {
    public static <T> WebJsonResponse<T> failed(String message) {
        return failed(message, status_other);
    }

    public static <T> WebJsonResponse<T> failed(String message, int status) {
        return new WebJsonResponse<>(status, message, null);
    }

    public static <T> WebJsonResponse<T> success(T t) {
        return new WebJsonResponse<>(status_success, "success", t);
    }

    public static final int status_other = -1;
    public static final int status_success = 0;
    public static final int status_timeout = 1;

}
