package com.virjar.crawler.extractor.classfetcher;

import org.apache.commons.lang3.StringUtils;
import org.htmlcleaner.TagNode;

import com.virjar.crawler.extractor.BaseSixtyfour;
import com.virjar.crawler.extractor.ClassFetcher;

/**
 * Created by virjar on 16/9/15.
 */
public class ProxyListOrgPortFetcher implements ClassFetcher {
    @Override
    public String fetcher(TagNode tagnode, int type) {
        CharSequence text = tagnode.getText();
        String s1 = new String(BaseSixtyfour.decode(StringUtils.substringBetween(text.toString(), "'", "'")));
        String[] split = s1.split(":");
        return split[1];
    }
}
