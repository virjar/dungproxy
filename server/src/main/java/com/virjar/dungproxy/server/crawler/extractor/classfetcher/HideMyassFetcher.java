package com.virjar.dungproxy.server.crawler.extractor.classfetcher;

import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Strings;
import org.apache.commons.lang3.StringUtils;
import org.htmlcleaner.TagNode;

import com.google.common.collect.Sets;
import com.virjar.dungproxy.server.crawler.extractor.ClassFetcher;

/**
 * Created by virjar on 16/11/26.
 */
public class HideMyassFetcher implements ClassFetcher {
    private static final Pattern stypePattern = Pattern.compile("\\.(.+)\\{display:(.+)\\}");

    @Override
    public String fetcher(TagNode tagnode, int type) {
        List<TagNode> childTagList = tagnode.getChildTagList();
        Set<String> dispalyAttr = Sets.newHashSet();
        Set<String> noneAttr = Sets.newHashSet();
        parseDisplayAttr(childTagList, dispalyAttr, noneAttr);
        StringBuilder sb = new StringBuilder(16);
        for (TagNode t : childTagList) {
            if (StringUtils.endsWithIgnoreCase(t.getName(), "style")) {
                continue;
            }
            if (StringUtils.contains(t.getAttributeByName("class"), "display:none")) {
                continue;
            }
            String aClass = t.getAttributeByName("class");
            if (!Strings.isNullOrEmpty(aClass) && noneAttr.contains(aClass)) {
                continue;
            }
            sb.append(t.getText().toString().trim());
        }

        return sb.toString();
    }

    private void parseDisplayAttr(List<TagNode> tagNodes, Set<String> displayAttr, Set<String> noneAttr) {
        for (TagNode t : tagNodes) {
            if (StringUtils.endsWithIgnoreCase(t.getName(), "style")) {
                Matcher matcher = stypePattern.matcher(t.getText());
                while (matcher.find()) {
                    String css = matcher.group(1);
                    String cssValue = matcher.group(2).trim();
                    if (StringUtils.endsWithIgnoreCase(cssValue, "none")) {
                        noneAttr.add(css);
                    } else {
                        displayAttr.add(css);
                    }
                }
                return;
            }
        }
    }
}
