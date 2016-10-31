package com.virjar.dungproxy.server.crawler.extractor.classfetcher;

import java.util.List;

import org.htmlcleaner.TagNode;

import com.virjar.dungproxy.server.crawler.extractor.ClassFetcher;

/*
 * http://ip.qiaodm.com/
 */
public class QianDmClassFetcher implements ClassFetcher {

    @Override
    public String fetcher(TagNode tagnode, int type) {
        /*
         * <td colspan="2"><span style='display:inline-block;'>11</span><p style='display:
         * none;'>1.</p><span>1.</span><div style='display:inline-block;'>1</div><div
         * style='display:inline-block;'>61</div><span style='display:inline-block;'>.</span><div
         * style='display:inline-block;'>1</div><span style='display:inline-block;'>2</span><span
         * style='display:inline-block;'></span><div style='display:inline-block;'></div><p style='display:
         * none;'>6.</p><span>6.</span><span style='display:inline-block;'>99</span></td>
         */
        StringBuilder sb = new StringBuilder();
        List<TagNode> childTagList = tagnode.getChildTagList();
        for (TagNode node : childTagList) {
            String style = node.getAttributeByName("style");
            if (style != null) {
                if (style.toLowerCase().contains("display: none;"))
                    continue;
            }
            sb.append(node.getText());
        }
        String ret = sb.toString();
        return ret.equals("") ? null : ret;
    }

}
