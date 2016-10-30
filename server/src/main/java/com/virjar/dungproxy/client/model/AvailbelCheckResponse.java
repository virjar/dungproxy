package com.virjar.model;

/**
 * Created by virjar on 16/8/14.
 */
public class AvailbelCheckResponse {
    public static final String staticKey = "taiye";
    private Byte transparent;
    private String key ;
    private String remoteAddr;
    private Long speed;
    private Byte type;
    private boolean lostHeader = false;
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

    public Long getSpeed() {
        return speed;
    }

    public void setSpeed(Long speed) {
        this.speed = speed;
    }

    public Byte getType() {
        return type;
    }

    public void setType(Byte type) {
        this.type = type;
    }

    public boolean isLostHeader() {
        return lostHeader;
    }

    public void setLostHeader(boolean lostHeader) {
        this.lostHeader = lostHeader;
    }
}
