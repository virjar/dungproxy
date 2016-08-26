package com.virjar.model;

/**
 * Created by virjar on 16/8/14.
 */
public class AvailbelCheckResponse {
    public static final String staticKey = "taiye";
    private Byte transparent;
    private String key ;
    private String remoteAddr;
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Byte getTransparent() {
        return transparent;
    }

    public void setTransparent(Byte transparent) {
        this.transparent = transparent;
    }

    public String getRemoteAddr() {
        return remoteAddr;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }
}
