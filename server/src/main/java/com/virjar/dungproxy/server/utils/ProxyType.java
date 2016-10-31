package com.virjar.dungproxy.server.utils;

public enum ProxyType {

    HTTP((byte) 1), HTTPS((byte) 2), HTTPHTTPS((byte) 3), SOCKET((byte) 4), VPN((byte) 5);
    private byte type;

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    ProxyType(byte type) {
        this.type = type;
    }

    public static ProxyType from(byte type) {
        for (ProxyType proxyType : ProxyType.values()) {
            if (proxyType.getType() == type) {
                return proxyType;
            }
        }
        return null;
    }
}
