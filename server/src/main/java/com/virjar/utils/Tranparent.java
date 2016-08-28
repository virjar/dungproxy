package com.virjar.utils;

/**
 * Created by virjar on 16/8/14.
 */
public enum Tranparent {
    highAnonymous((byte) 0), anonymous((byte)1), transparent((byte)2);

    Byte value;

    Tranparent(Byte value) {
        this.value = value;
    }

    public Byte getValue() {
        return value;
    }
}
