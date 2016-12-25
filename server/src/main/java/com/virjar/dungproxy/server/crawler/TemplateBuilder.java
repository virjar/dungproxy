package com.virjar.dungproxy.server.crawler;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.virjar.dungproxy.client.ippool.config.ObjectFactory;
import com.virjar.dungproxy.server.crawler.extractor.XmlModeFetcher;
import com.virjar.dungproxy.server.crawler.impl.TemplateCollector;
import com.virjar.dungproxy.server.crawler.urlgenerator.WildCardURLGenerator;

/**
 * Created by virjar on 16/11/26.
 */
public class TemplateBuilder {
    private static final Logger logger = LoggerFactory.getLogger(TemplateBuilder.class);

    public static List<TemplateCollector> buildfromSource(String source) {
        if (source == null) {
            source = "/handmapper.xml";
        }
        List<TemplateCollector> ret = new ArrayList<>();
        Document parseText = null;
        try {
            parseText = DocumentHelper.parseText(IOUtils.toString(TemplateBuilder.class.getResourceAsStream(source)));
        } catch (Exception e) {
            logger.error("加载模版抽取规则失败 :{}", source, e);
            return ret;
        }
        Element rootElement = parseText.getRootElement();
        if (rootElement != null && "handmapers".equals(rootElement.getName())) {
            Iterator<Element> elementIterator = rootElement.elementIterator();
            while (elementIterator.hasNext()) {
                Element next = elementIterator.next();
                if ("maper".equals(next.getName())) {
                    try {
                        if (!"true".equals(next.element("enable").getTextTrim())) {
                            continue;
                        }
                        TemplateCollector collector = new TemplateCollector();
                        String hierate = next.elementText("hierate");
                        collector.setDuration(parseTime(hierate));

                        String bachSize = next.elementText("batchsize");
                        if (bachSize != null) {
                            collector.setBatchSize(NumberUtils.toInt(bachSize, 10));
                        }
                        collector.setSuccessKey(next.elementText("successKey"));

                        collector.setFetcher(new XmlModeFetcher(IOUtils
                                .toString(TemplateBuilder.class.getResourceAsStream(next.elementText("fetcher")))));
                        Element classgenerator = next.element("classgenerator");
                        boolean hasagenerator = false;
                        if (classgenerator != null && !"".equals(classgenerator.getTextTrim())) {
                            collector.setUrlGenerator(
                                    (URLGenerator) ObjectFactory.newInstance(classgenerator.getTextTrim()));
                            hasagenerator = true;
                        }
                        Element wildcardgenerator = next.element("wildcardgenerator");
                        if (!hasagenerator && wildcardgenerator != null
                                && !"".equals(wildcardgenerator.getTextTrim())) {
                            collector.setUrlGenerator(new WildCardURLGenerator(wildcardgenerator.getTextTrim()));
                            hasagenerator = true;
                        }

                        Element maxPage = next.element("maxPage");

                        if (hasagenerator) {
                            if (maxPage != null) {//最大页数阀值
                                collector.getUrlGenerator()
                                        .setMaxPage(NumberUtils.toInt(maxPage.getTextTrim(), Integer.MAX_VALUE));
                            }
                            ret.add(collector);
                        }
                    } catch (Exception e) {
                        logger.error("error when build template collector", e);
                    }
                }
            }
        }
        return ret;
    }

    private static int parseTime(String str) {
        if (str == null)
            return 0;
        String newStr = str.toLowerCase();
        try {
            if (newStr.endsWith("h")) {
                return Integer.parseInt(newStr.substring(0, newStr.length() - 1)) * 60;
            } else if (newStr.endsWith("m")) {
                return Integer.parseInt(newStr.substring(0, newStr.length() - 1));
            }
        } catch (NumberFormatException e) {
        }
        return 120;
    }
}
