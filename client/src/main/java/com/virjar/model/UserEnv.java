package com.virjar.model;

/**
 * 用于存放用户信息<br/>
 * Created by virjar on 16/10/1.
 */
public class UserEnv {
    private Object user;
    private AvProxy bindProxy;

    public AvProxy getBindProxy() {
        return bindProxy;
    }

    public void setBindProxy(AvProxy bindProxy) {
        this.bindProxy = bindProxy;
    }

    public Object getUser() {
        return user;
    }

    public void setUser(Object user) {
        this.user = user;
    }
}
