package com.virjar.dungproxy.server.crawler.impl;

import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.virjar.dungproxy.client.httpclient.CrawlerHttpClient;
import com.virjar.dungproxy.client.httpclient.HttpInvoker;
import com.virjar.dungproxy.server.crawler.NewCollector;
import com.virjar.dungproxy.server.entity.Proxy;

/**
 * Created by virjar on 17/3/19.
 */
@Component
public class KuaiDailiCollector extends NewCollector {
    private ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript");
    private Invocable invo = (Invocable) engine
            .eval(String.valueOf(KuaiDailiCollector.class.getResourceAsStream("/js/kuaidaili.js")));
    private CrawlerHttpClient crawlerHttpClient = HttpInvoker.buildDefault();
    private static final String baseURL = "http://www.kuaidaili.com/free/inha/";

    public KuaiDailiCollector() throws ScriptException {
    }

    @Override
    public String lasUrl() {
        return null;
    }

    @Override
    public List<Proxy> doCollect() {
        int page = 1, maxPage = 30;// 只抓30页,这样不管是否翻页了
        for (; page < maxPage; page++) {
            String url = baseURL + page + "/";
        }
        return null;
    }

    private String download(String url) {
        for (int i = 0; i < 3; i++) {
            String s = crawlerHttpClient.get(url);
            if (StringUtils.contains(s, "快代理专业为您提供国内高匿免费HTTP代理服务器")) {
                return s;
            }
            // 异常情况
            if (StringUtils.contains(s, "eval(\"qo=eval;qo(po);\")")) {
                // 存在脚本
                String tb = StringUtils.substringBetween(s, "window.onload=setTimeout(\"aw(", ")\",");
                String oo = StringUtils.substringBetween(s, "var qo, mo=\"\", no=\"\", oo = ", ";");
                try {
                    String aw = (String) invo.invokeFunction("aw", tb, oo);
                    System.out.println(aw);
                } catch (ScriptException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                    // 不会发生
                }
            }
        }

        return null;
    }
}
