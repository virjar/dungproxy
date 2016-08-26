package com.virjar.utils;

public enum ProxyType {

    HTTP(1), HTTPS(2), HTTPHTTPS(3), SOCKET(4), VPN(5);
    private int type;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    ProxyType(int type) {
        this.type = type;
    }

}
