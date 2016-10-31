package com.virjar.dungproxy.server.crawler.extractor.classfetcher;

import org.apache.commons.lang3.StringUtils;
import org.htmlcleaner.TagNode;

import com.virjar.dungproxy.server.crawler.extractor.ClassFetcher;

/**
 * 通过这种方式能够拿到IP,但是端口号存在js加密,直接走端口探测模块吧 Created by virjar on 16/9/15.
 */
public class GoubanjiaClassFetcher implements ClassFetcher {
    @Override
    public String fetcher(TagNode tagnode, int type) {
        StringBuilder sb = new StringBuilder();
        for (TagNode tagNode : tagnode.getChildTagList()) {
            if (StringUtils.contains(trimBlank(tagNode.getAttributeByName("style")), "display:none")) {
                continue;
            }
            sb.append(tagNode.getText());
        }
        return sb.toString().trim();
    }

    private String trimBlank(String input) {
        if (input == null) {
            return null;
        }
        return input.replaceAll("\\s", "");
    }
}
