package com.virjar.dungproxy.server.crawler.extractor;

import org.htmlcleaner.TagNode;

public interface ClassFetcher {

    String fetcher(TagNode tagnode, int type);
}
