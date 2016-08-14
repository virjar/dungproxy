package com.virjar.utils;

/**
 * Created by virjar on 16/8/14.
 */
public enum Tranparent {
    highAnonymous(0), anonymous(1), transparent(2);
    int value;

    Tranparent(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
