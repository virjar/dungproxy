package com.virjar.dungproxy.client.samples.webmagic.dytt8;

import com.virjar.dungproxy.client.ippool.PreHeater;
import com.virjar.dungproxy.client.ippool.config.ProxyConstant;

/**
 * Created by virjar on 16/11/28.
 */
public class PreHeatertest {
    public static void main(String[] args) {
        ProxyConstant.CLIENT_CONFIG_FILE_NAME ="proxyclient_dytt8.properties";//加载电影天堂的配置
        PreHeater.start();
    }
}
