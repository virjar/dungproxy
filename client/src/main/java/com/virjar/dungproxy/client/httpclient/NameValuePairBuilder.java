package com.virjar.dungproxy.client.httpclient;

import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import com.google.common.collect.Lists;

/**
 * Created by virjar on 17/1/20.<br/>
 * nameValuePair可以保证参数序列化顺序,同时允许key相同(传递到服务器可以将多个key相同的数据转化为数组,根据nameValuePair的顺序)
 */
public class NameValuePairBuilder {
    private List<NameValuePair> params = Lists.newArrayList();

    public static NameValuePairBuilder create() {
        return new NameValuePairBuilder();
    }

    public NameValuePairBuilder addParam(String name, String value) {
        params.add(new BasicNameValuePair(name, value));
        return this;
    }

    public NameValuePairBuilder addParam(String name) {
        return addParam(name, "");
    }

    public List<NameValuePair> build() {
        return params;
    }
}
