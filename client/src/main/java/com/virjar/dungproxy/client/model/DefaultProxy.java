package com.virjar.dungproxy.client.model;

/**
 * Created by virjar on 16/10/29.
 */
public class DefaultProxy extends AvProxy {
    @Override
    public void offline() {
        // do nothing 默认代理永不下线?
    }
}
