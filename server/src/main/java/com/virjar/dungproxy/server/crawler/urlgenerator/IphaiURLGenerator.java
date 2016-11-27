package com.virjar.dungproxy.server.crawler.urlgenerator;

import java.util.List;

import com.google.common.collect.Lists;
import com.virjar.dungproxy.server.crawler.URLGenerator;

/**
 * Created by virjar on 16/11/27.
 */
public class IphaiURLGenerator extends URLGenerator {
    private List<String> urls = Lists.newArrayList();
    private int index = 0;

    public IphaiURLGenerator() {
        urls.add("http://www.iphai.com/");
        urls.add("http://www.iphai.com/free/ng");
        urls.add("http://www.iphai.com/free/np");
        urls.add("http://www.iphai.com/free/wg");
        urls.add("http://www.iphai.com/free/wp");
    }

    @Override
    public String newURL() {
        String ret = urls.get(index % urls.size());
        if (index > urls.size()) {
            index = 0;
        }
        return ret;
    }
}
