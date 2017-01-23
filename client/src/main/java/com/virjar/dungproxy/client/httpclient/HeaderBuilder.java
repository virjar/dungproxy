package com.virjar.dungproxy.client.httpclient;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import com.google.common.collect.Lists;

/**
 * Created by virjar on 17/1/23.
 */
public class HeaderBuilder {
    private List<Header> headerList = Lists.newArrayList();

    public static HeaderBuilder create() {
        return new HeaderBuilder();
    }

    public HeaderBuilder defaultCommonHeader() {
        headerList.add(new BasicHeader("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8"));
        headerList.add(new BasicHeader("Accept-Language", "zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3"));
        headerList.add(new BasicHeader("Accept-Encoding", "gzip, deflate"));
        headerList.add(new BasicHeader("Connection", "keep-alive"));
        headerList.add(new BasicHeader("Cache-Control", "no-cache"));
        headerList.add(new BasicHeader("Pragma", "no-cache"));
        return this;
    }

    /**
     * 提交表单数据大的时候,需要添加这个头部
     * 
     * @return HeaderBuilder
     */
    public HeaderBuilder withFormData() {
        removeHeader("Content-Type");
        headerList.add(new BasicHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"));
        return this;
    }

    public HeaderBuilder withUA(String userAgent) {
        removeHeader("User-Agent");
        headerList.add(new BasicHeader("User-Agent", userAgent));
        return this;
    }

    public HeaderBuilder withRefer(String refer) {
        removeHeader("Referer");
        headerList.add(new BasicHeader("Referer", refer));
        return this;
    }

    public HeaderBuilder withRandomUA() {
        return withUA(randomUserAgent());
    }

    public HeaderBuilder withSingleHeader(String key, String value) {
        removeHeader(key);
        headerList.add(new BasicHeader(key, value));
        return this;
    }

    public HeaderBuilder withHeader(String key, String value) {
        headerList.add(new BasicHeader(key, value));
        return this;
    }

    public Header[] buildArray() {
        return headerList.toArray(new Header[] {});
    }

    public List<Header> buildList() {
        return Lists.newArrayList(headerList);
    }

    public HeaderBuilder removeHeader(String key) {
        Iterator<Header> iterator = headerList.iterator();
        while (iterator.hasNext()) {
            Header next = iterator.next();
            if (next.getName().equals(key)) {
                iterator.remove();
            }
        }
        return this;
    }

    public static String randomUserAgent() {

        Random random = new Random();

        String userAgent = "";

        int webBrowser = random.nextInt(2);
        switch (webBrowser) {
        // Chrome
        case 0:
            userAgent = buildChrome();
            break;
        // Firfox
        case 1:
            userAgent = buildFirfox();
            break;
        default:
            userAgent = buildChrome();
        }
        return userAgent;
    }

    /**
     * Chrome UA
     */
    private static String buildChrome() {

        // Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/55.0.2883.95
        // Safari/537.36
        Random random = new Random();

        StringBuilder builder = new StringBuilder();
        builder.append("Mozilla/");
        builder.append(random.nextInt(3) + 3);
        builder.append(" (X");
        builder.append(random.nextInt(10) + 5);
        builder.append(" Windows) AppleWebKit/");
        int mV = random.nextInt(50) + 510;
        int sV = random.nextInt(80) + 5;
        builder.append(mV);
        builder.append(".");
        builder.append(sV);
        builder.append(" (KHTML, like Gecko)");
        builder.append(" Chrome/");
        builder.append(chromeVersions.get(random.nextInt(chromeVersions.size())));

        builder.append(" Safari/");
        builder.append(mV);
        builder.append(".");
        builder.append(sV);
        return builder.toString();
    }

    /**
     * Firfox UA
     */
    private static String buildFirfox() {

        Random random = new Random();

        StringBuilder builder = new StringBuilder();
        builder.append("Mozilla/");
        builder.append(random.nextInt(3) + 3);
        builder.append(" (X");
        builder.append(random.nextInt(10) + 5);
        builder.append(" Window NT ");
        builder.append(random.nextInt(10));
        builder.append(".");
        builder.append(random.nextInt(10));
        builder.append(" rv:");

        int mV = random.nextInt(30) + 15;
        int sV = random.nextInt(20) + 10;
        int lV = random.nextInt(10);

        builder.append(mV);
        builder.append(".");
        builder.append(sV);
        builder.append(".");
        builder.append(lV);
        builder.append(") Gecko/20100101 Firefox/");
        builder.append(mV);
        builder.append(".");
        builder.append(sV);
        builder.append(".");
        builder.append(lV);

        builder.append(" Firefox/");
        builder.append(firfoxVersions.get(random.nextInt(firfoxVersions.size())));

        return builder.toString();
    }

    private static List<String> chromeVersions = Lists.newArrayList();
    private static List<String> firfoxVersions = Lists.newArrayList();
    static {
        chromeVersions.add("55.0.2883.95");
        chromeVersions.add("52.0.2743.116");
        chromeVersions.add("52.0.2743.116");
        chromeVersions.add("52.0.2743.82");
        chromeVersions.add("52.0.2743.82");
        chromeVersions.add("46.0.2490.86");
        chromeVersions.add("46.0.2490.86");
        chromeVersions.add("45.0.2454.85");
        chromeVersions.add("45.0.2454.85");
        chromeVersions.add("44.0.2403.157");
        chromeVersions.add("44.0.2403.157");
        chromeVersions.add("41.0.2272.89");
        chromeVersions.add("41.0.2272.89");
        chromeVersions.add("39.0.2171.75");
        chromeVersions.add("39.0.2171.75");
        chromeVersions.add("39.0.2171.71");
        chromeVersions.add("39.0.2171.71");
        chromeVersions.add("39.0.2171.65");
        chromeVersions.add("38.0.2125.122");
        chromeVersions.add("36.0.1985.125");
        chromeVersions.add("35.0.1916.114");
        chromeVersions.add("34.0.1847.116");
        chromeVersions.add("33.0.1750.117");
        chromeVersions.add("32.0.1700.76");
        chromeVersions.add("31.0.1650.63");
        chromeVersions.add("31.0.1650.57");
        chromeVersions.add("31.0.1650.48");
        chromeVersions.add("30.0.1599.101");
        chromeVersions.add("30.0.1599.69");
        chromeVersions.add("29.0.1547.66");
        chromeVersions.add("29.0.1547.76");
        chromeVersions.add("29.0.1547.62");
        chromeVersions.add("29.0.1547.57");
        chromeVersions.add("28.0.1500.95");
        chromeVersions.add("28.0.1500.72");
        chromeVersions.add("28.0.1500.71");
        chromeVersions.add("27.0.1453.116");
        chromeVersions.add("27.0.1453.110");
        chromeVersions.add("27.0.1453.93");
        chromeVersions.add("26.0.1410.64");
        chromeVersions.add("26.0.1410.43");
        chromeVersions.add("25.0.1364.160");
        chromeVersions.add("25.0.1364.152");
        chromeVersions.add("24.0.1312.56");
        chromeVersions.add("24.0.1312.52");
        chromeVersions.add("23.0.1271.97");
        chromeVersions.add("23.0.1271.95");
        chromeVersions.add("23.0.1271.91");
        chromeVersions.add("23.0.1271.64");
        chromeVersions.add("22.0.1229.96");
        chromeVersions.add("22.0.1229.94");
        chromeVersions.add("22.0.1229.79");
        chromeVersions.add("21.0.1180.89");
        chromeVersions.add("21.0.1180.83");
        chromeVersions.add("21.0.1180.79");
        chromeVersions.add("21.0.1180.60");
        chromeVersions.add("20.0.1132.57");
        chromeVersions.add("20.0.1132.47");
        chromeVersions.add("19.0.1084.56");
        chromeVersions.add("19.0.1084.52");
        chromeVersions.add("19.0.1084.46");
        chromeVersions.add("18.0.1025.162");
        chromeVersions.add("18.0.1025.151");
        chromeVersions.add("18.0.1025.140");
        chromeVersions.add("17.0.963.79");
        chromeVersions.add("17.0.963.65");
        chromeVersions.add("17.0.963.46");
        chromeVersions.add("35.0.1916.114");
        chromeVersions.add("38.0.2125.122");
        chromeVersions.add("39.0.2171.65");
        chromeVersions.add("33.0.1750.154");

        firfoxVersions.add("20.0");
        firfoxVersions.add("20.0b1");
        firfoxVersions.add("20.0b2");
        firfoxVersions.add("20.0b3");
        firfoxVersions.add("20.0b4-funnelcake19");
        firfoxVersions.add("20.0b4-funnelcake20");
        firfoxVersions.add("20.0b4");
        firfoxVersions.add("20.0b5");
        firfoxVersions.add("20.0b6");
        firfoxVersions.add("20.0b7");
        firfoxVersions.add("21.0");
        firfoxVersions.add("21.0b1");
        firfoxVersions.add("21.0b2");
        firfoxVersions.add("21.0b3");
        firfoxVersions.add("21.0b4");
        firfoxVersions.add("21.0b5");
        firfoxVersions.add("21.0b6");
        firfoxVersions.add("21.0b7");
        firfoxVersions.add("22.0-funnelcake23");
        firfoxVersions.add("22.0");
        firfoxVersions.add("22.0b1");
        firfoxVersions.add("22.0b2");
        firfoxVersions.add("22.0b3");
        firfoxVersions.add("22.0b4");
        firfoxVersions.add("22.0b5");
        firfoxVersions.add("22.0b6");
        firfoxVersions.add("23.0.1");
        firfoxVersions.add("23.0");
        firfoxVersions.add("23.0b1");
        firfoxVersions.add("23.0b10");
        firfoxVersions.add("23.0b2");
        firfoxVersions.add("23.0b3");
        firfoxVersions.add("23.0b4");
        firfoxVersions.add("23.0b5");
        firfoxVersions.add("23.0b6");
        firfoxVersions.add("23.0b7");
        firfoxVersions.add("23.0b8");
        firfoxVersions.add("23.0b9");
        firfoxVersions.add("24.0-funnelcake24");
        firfoxVersions.add("24.0-funnelcake25");
        firfoxVersions.add("24.0");
        firfoxVersions.add("24.0b1");
        firfoxVersions.add("24.0b10");
        firfoxVersions.add("24.0b2");
        firfoxVersions.add("24.0b3");
        firfoxVersions.add("24.0b4");
        firfoxVersions.add("24.0b5");
        firfoxVersions.add("24.0b6");
        firfoxVersions.add("24.0b7");
        firfoxVersions.add("24.0b8");
        firfoxVersions.add("24.0b9");
        firfoxVersions.add("24.0esr");
        firfoxVersions.add("24.1.0esr");
        firfoxVersions.add("24.1.1esr");
        firfoxVersions.add("24.2.0esr");
        firfoxVersions.add("24.3.0esr");
        firfoxVersions.add("24.4.0esr");
        firfoxVersions.add("24.5.0esr");
        firfoxVersions.add("24.6.0esr");
        firfoxVersions.add("24.7.0esr");
        firfoxVersions.add("24.8.0esr");
        firfoxVersions.add("24.8.1esr");
        firfoxVersions.add("25.0.1");
        firfoxVersions.add("25.0");
        firfoxVersions.add("25.0b1");
        firfoxVersions.add("25.0b10");
        firfoxVersions.add("25.0b11");
        firfoxVersions.add("25.0b12");
        firfoxVersions.add("25.0b2");
        firfoxVersions.add("25.0b3");
        firfoxVersions.add("25.0b4");
        firfoxVersions.add("25.0b6");
        firfoxVersions.add("25.0b7");
        firfoxVersions.add("25.0b8");
        firfoxVersions.add("25.0b9");
        firfoxVersions.add("26.0");
        firfoxVersions.add("26.0b1");
        firfoxVersions.add("26.0b10");
        firfoxVersions.add("26.0b2");
        firfoxVersions.add("26.0b3");
        firfoxVersions.add("26.0b4");
        firfoxVersions.add("26.0b5");
        firfoxVersions.add("26.0b6");
        firfoxVersions.add("26.0b7");
        firfoxVersions.add("26.0b8");
        firfoxVersions.add("27.0.1");
        firfoxVersions.add("27.0");
        firfoxVersions.add("27.0b1");
        firfoxVersions.add("27.0b2");
        firfoxVersions.add("27.0b4");
        firfoxVersions.add("27.0b5");
        firfoxVersions.add("27.0b6");
        firfoxVersions.add("27.0b7");
        firfoxVersions.add("27.0b8");
        firfoxVersions.add("27.0b9");
        firfoxVersions.add("28.0");
        firfoxVersions.add("28.0b1");
        firfoxVersions.add("28.0b2");
        firfoxVersions.add("28.0b3");
        firfoxVersions.add("28.0b4");
        firfoxVersions.add("28.0b6");
        firfoxVersions.add("28.0b7");
        firfoxVersions.add("28.0b8");
        firfoxVersions.add("28.0b9");
        firfoxVersions.add("29.0-funnelcake30");
        firfoxVersions.add("29.0-funnelcake31");
        firfoxVersions.add("29.0.1");
        firfoxVersions.add("29.0");
        firfoxVersions.add("29.0b1");
        firfoxVersions.add("29.0b2");
        firfoxVersions.add("29.0b3");
        firfoxVersions.add("29.0b4");
        firfoxVersions.add("29.0b5");
        firfoxVersions.add("29.0b6");
        firfoxVersions.add("29.0b7");
        firfoxVersions.add("29.0b8");
        firfoxVersions.add("29.0b9");
        firfoxVersions.add("3.0.1");
        firfoxVersions.add("3.0.10");
        firfoxVersions.add("3.0.11");
        firfoxVersions.add("3.0.12");
        firfoxVersions.add("3.0.13");
        firfoxVersions.add("3.0.14");
        firfoxVersions.add("3.0.15");
        firfoxVersions.add("3.0.16-real");
        firfoxVersions.add("3.0.17");
        firfoxVersions.add("3.0.18");
        firfoxVersions.add("3.0.19-real-real");
        firfoxVersions.add("3.0.2");
        firfoxVersions.add("3.0.3");
        firfoxVersions.add("3.0.4");
        firfoxVersions.add("3.0.5");
        firfoxVersions.add("3.0.6");
        firfoxVersions.add("3.0.7");
        firfoxVersions.add("3.0.8");
        firfoxVersions.add("3.0.9");
        firfoxVersions.add("3.0");
        firfoxVersions.add("3.0b1");
        firfoxVersions.add("3.0b2");
        firfoxVersions.add("3.0b3");
        firfoxVersions.add("3.0b4");
        firfoxVersions.add("3.0b5");
        firfoxVersions.add("3.0rc1");
        firfoxVersions.add("3.0rc2");
        firfoxVersions.add("3.0rc3");
        firfoxVersions.add("3.1b1");
        firfoxVersions.add("3.1b2");
        firfoxVersions.add("3.1b3");
        firfoxVersions.add("3.5.1");
        firfoxVersions.add("3.5.10");
        firfoxVersions.add("3.5.11");
        firfoxVersions.add("3.5.12");
        firfoxVersions.add("3.5.13");
        firfoxVersions.add("3.5.14");
        firfoxVersions.add("3.5.15");
        firfoxVersions.add("3.5.16");
        firfoxVersions.add("3.5.17");
        firfoxVersions.add("3.5.18");
        firfoxVersions.add("3.5.19");
        firfoxVersions.add("3.5.2");
        firfoxVersions.add("3.5.3");
        firfoxVersions.add("3.5.4");
        firfoxVersions.add("3.5.5");
        firfoxVersions.add("3.5.6");
        firfoxVersions.add("3.5.7");
        firfoxVersions.add("3.5.8");
        firfoxVersions.add("3.5.9");
        firfoxVersions.add("3.5");
        firfoxVersions.add("3.5b4");
        firfoxVersions.add("3.5b99");
        firfoxVersions.add("3.5rc1");
        firfoxVersions.add("3.5rc2");
        firfoxVersions.add("3.5rc3");
        firfoxVersions.add("3.6.10");
        firfoxVersions.add("3.6.11");
        firfoxVersions.add("3.6.12");
        firfoxVersions.add("3.6.13");
        firfoxVersions.add("3.6.14");
        firfoxVersions.add("3.6.15");
        firfoxVersions.add("3.6.16");
        firfoxVersions.add("3.6.17");
        firfoxVersions.add("3.6.18");
        firfoxVersions.add("3.6.19");
        firfoxVersions.add("3.6.2");
        firfoxVersions.add("3.6.20");
        firfoxVersions.add("3.6.21");
        firfoxVersions.add("3.6.22");
        firfoxVersions.add("3.6.23");
        firfoxVersions.add("3.6.24");
        firfoxVersions.add("3.6.25");
        firfoxVersions.add("3.6.26");
        firfoxVersions.add("3.6.27");
        firfoxVersions.add("3.6.28");
        firfoxVersions.add("3.6.3");
        firfoxVersions.add("3.6.3plugin1");
        firfoxVersions.add("3.6.4");
        firfoxVersions.add("3.6.6");
        firfoxVersions.add("3.6.7");
        firfoxVersions.add("3.6.8");
        firfoxVersions.add("3.6.9");
        firfoxVersions.add("3.6");
        firfoxVersions.add("3.6b1");
        firfoxVersions.add("3.6b2");
        firfoxVersions.add("3.6b3");
        firfoxVersions.add("3.6b4");
        firfoxVersions.add("3.6b5");
        firfoxVersions.add("3.6rc1");
        firfoxVersions.add("3.6rc2");
        firfoxVersions.add("30.0");
        firfoxVersions.add("30.0b1");
        firfoxVersions.add("30.0b2");
        firfoxVersions.add("30.0b3");
        firfoxVersions.add("30.0b4");
        firfoxVersions.add("30.0b5");
        firfoxVersions.add("30.0b6");
        firfoxVersions.add("30.0b7");
        firfoxVersions.add("30.0b8");
        firfoxVersions.add("30.0b9");
        firfoxVersions.add("31.0");
        firfoxVersions.add("31.0b1");
        firfoxVersions.add("31.0b2");
        firfoxVersions.add("31.0b3");
        firfoxVersions.add("31.0b4");
        firfoxVersions.add("31.0b5");
        firfoxVersions.add("31.0b6");
        firfoxVersions.add("31.0b7");
        firfoxVersions.add("31.0b8");
        firfoxVersions.add("31.0b9");
        firfoxVersions.add("31.0esr");
        firfoxVersions.add("31.1.0esr");
        firfoxVersions.add("31.1.1esr");
        firfoxVersions.add("31.2.0esr");
        firfoxVersions.add("31.3.0esr");
        firfoxVersions.add("31.4.0esr");
        firfoxVersions.add("31.5.0esr");
        firfoxVersions.add("31.5.1esr");
        firfoxVersions.add("31.5.2esr");
        firfoxVersions.add("31.5.3esr");
        firfoxVersions.add("31.6.0esr");
        firfoxVersions.add("31.7.0esr");
        firfoxVersions.add("31.8.0esr");
        firfoxVersions.add("32.0.1");
        firfoxVersions.add("32.0.2");
        firfoxVersions.add("32.0.3");
        firfoxVersions.add("32.0");
        firfoxVersions.add("32.0b1");
        firfoxVersions.add("32.0b2");
        firfoxVersions.add("32.0b3");
        firfoxVersions.add("32.0b4");
        firfoxVersions.add("32.0b5");
        firfoxVersions.add("32.0b6");
        firfoxVersions.add("32.0b7");
        firfoxVersions.add("32.0b8");
        firfoxVersions.add("32.0b9");
        firfoxVersions.add("33.0.1");
        firfoxVersions.add("33.0.2");
        firfoxVersions.add("33.0.3");
        firfoxVersions.add("33.0");
        firfoxVersions.add("33.0b1");
        firfoxVersions.add("33.0b2");
        firfoxVersions.add("33.0b3");
        firfoxVersions.add("33.0b4");
        firfoxVersions.add("33.0b5");
        firfoxVersions.add("33.0b6");
        firfoxVersions.add("33.0b7");
        firfoxVersions.add("33.0b8");
        firfoxVersions.add("33.0b9");
        firfoxVersions.add("33.1.1");
        firfoxVersions.add("33.1");
        firfoxVersions.add("34.0.5");
        firfoxVersions.add("34.0");
        firfoxVersions.add("34.0b1");
        firfoxVersions.add("34.0b10");
        firfoxVersions.add("34.0b11");
        firfoxVersions.add("34.0b2");
        firfoxVersions.add("34.0b3");
        firfoxVersions.add("34.0b4");
        firfoxVersions.add("34.0b5");
        firfoxVersions.add("34.0b6");
        firfoxVersions.add("34.0b7");
        firfoxVersions.add("34.0b8");
        firfoxVersions.add("34.0b9");
        firfoxVersions.add("35.0.1-funnelcake32");
        firfoxVersions.add("35.0.1-funnelcake33");
        firfoxVersions.add("35.0.1-funnelcake34");
        firfoxVersions.add("35.0.1-funnelcake35");
        firfoxVersions.add("35.0.1-funnelcake36");
        firfoxVersions.add("35.0.1");
        firfoxVersions.add("35.0");
        firfoxVersions.add("35.0b1");
        firfoxVersions.add("35.0b2");
        firfoxVersions.add("35.0b3");
        firfoxVersions.add("35.0b4");
        firfoxVersions.add("35.0b5");
        firfoxVersions.add("35.0b6");
        firfoxVersions.add("35.0b8");
        firfoxVersions.add("36.0.1");
        firfoxVersions.add("36.0.3");
        firfoxVersions.add("36.0.4");
        firfoxVersions.add("36.0");
        firfoxVersions.add("36.0b1");
        firfoxVersions.add("36.0b10");
        firfoxVersions.add("36.0b2");
        firfoxVersions.add("36.0b3");
        firfoxVersions.add("36.0b4");
        firfoxVersions.add("36.0b5");
        firfoxVersions.add("36.0b6");
        firfoxVersions.add("36.0b7");
        firfoxVersions.add("36.0b8");
        firfoxVersions.add("36.0b9");
        firfoxVersions.add("37.0.1");
        firfoxVersions.add("37.0.2");
        firfoxVersions.add("37.0");
        firfoxVersions.add("37.0b1");
        firfoxVersions.add("37.0b2");
        firfoxVersions.add("37.0b3");
        firfoxVersions.add("37.0b4");
        firfoxVersions.add("37.0b5");
        firfoxVersions.add("37.0b6");
        firfoxVersions.add("37.0b7");
        firfoxVersions.add("38.0.1");
        firfoxVersions.add("38.0.1esr");
        firfoxVersions.add("38.0.5-funnelcake37");
        firfoxVersions.add("38.0.5-funnelcake38");
        firfoxVersions.add("38.0.5-funnelcake39");
        firfoxVersions.add("38.0.5");
        firfoxVersions.add("38.0.5b1-2");
        firfoxVersions.add("38.0.5b1");
        firfoxVersions.add("38.0.5b2");
        firfoxVersions.add("38.0.5b3");
        firfoxVersions.add("38.0.6");
        firfoxVersions.add("38.0");
        firfoxVersions.add("38.0b1");
        firfoxVersions.add("38.0b2");
        firfoxVersions.add("38.0b3");
        firfoxVersions.add("38.0b4");
        firfoxVersions.add("38.0b5");
        firfoxVersions.add("38.0b6");
        firfoxVersions.add("38.0b7");
        firfoxVersions.add("38.0b8-2");
        firfoxVersions.add("38.0b8");
        firfoxVersions.add("38.0b9");
        firfoxVersions.add("38.0esr");
        firfoxVersions.add("38.1.0esr");
        firfoxVersions.add("38.1.1esr");
        firfoxVersions.add("38.2.0esr");
        firfoxVersions.add("38.2.1esr");
        firfoxVersions.add("38.3.0esr");
        firfoxVersions.add("38.4.0esr");
        firfoxVersions.add("38.5.0esr");
        firfoxVersions.add("38.5.1esr");
        firfoxVersions.add("38.5.2esr");
        firfoxVersions.add("38.6.0esr");
        firfoxVersions.add("38.6.1esr");
        firfoxVersions.add("38.7.0esr");
        firfoxVersions.add("38.7.1esr");
        firfoxVersions.add("38.8.0esr");
        firfoxVersions.add("39.0-funnelcake40");
        firfoxVersions.add("39.0-funnelcake41");
        firfoxVersions.add("39.0.3");
        firfoxVersions.add("39.0");
        firfoxVersions.add("39.0b1");
        firfoxVersions.add("39.0b2");
        firfoxVersions.add("39.0b3");
        firfoxVersions.add("39.0b4");
        firfoxVersions.add("39.0b5");
        firfoxVersions.add("39.0b6");
        firfoxVersions.add("39.0b7");
        firfoxVersions.add("4.0.1");
        firfoxVersions.add("4.0");
        firfoxVersions.add("4.0b1");
        firfoxVersions.add("4.0b10");
        firfoxVersions.add("4.0b11");
        firfoxVersions.add("4.0b12");
        firfoxVersions.add("4.0b2");
        firfoxVersions.add("4.0b3");
        firfoxVersions.add("4.0b4");
        firfoxVersions.add("4.0b5");
        firfoxVersions.add("4.0b6");
        firfoxVersions.add("4.0b7");
        firfoxVersions.add("4.0b8");
        firfoxVersions.add("4.0b9");
        firfoxVersions.add("4.0rc1");
        firfoxVersions.add("4.0rc2");
        firfoxVersions.add("40.0.2-funnelcake44");
        firfoxVersions.add("40.0.2");
        firfoxVersions.add("40.0.3-funnelcake42");
        firfoxVersions.add("40.0.3-funnelcake43");
        firfoxVersions.add("40.0.3-funnelcake44");
        firfoxVersions.add("40.0.3-funnelcake45");
        firfoxVersions.add("40.0.3-funnelcake46");
        firfoxVersions.add("40.0.3-funnelcake47");
        firfoxVersions.add("40.0.3-funnelcake48");
        firfoxVersions.add("40.0.3-funnelcake49");
        firfoxVersions.add("40.0.3");
        firfoxVersions.add("40.0");
        firfoxVersions.add("40.0b1");
        firfoxVersions.add("40.0b2");
        firfoxVersions.add("40.0b3");
        firfoxVersions.add("40.0b4");
        firfoxVersions.add("40.0b6");
        firfoxVersions.add("40.0b7");
        firfoxVersions.add("40.0b8");
        firfoxVersions.add("40.0b9");
        firfoxVersions.add("41.0.1-funnelcake50");
        firfoxVersions.add("41.0.1-funnelcake51");
        firfoxVersions.add("41.0.1-funnelcake52");
        firfoxVersions.add("41.0.1-funnelcake53");
        firfoxVersions.add("41.0.1");
        firfoxVersions.add("41.0.2-funnelcake50");
        firfoxVersions.add("41.0.2-funnelcake51");
        firfoxVersions.add("41.0.2-funnelcake52");
        firfoxVersions.add("41.0.2-funnelcake53");
        firfoxVersions.add("41.0.2");
        firfoxVersions.add("41.0");
        firfoxVersions.add("41.0b1");
        firfoxVersions.add("41.0b2");
        firfoxVersions.add("41.0b3");
        firfoxVersions.add("41.0b4");
        firfoxVersions.add("41.0b5");
        firfoxVersions.add("41.0b6");
        firfoxVersions.add("41.0b7");
        firfoxVersions.add("41.0b8");
        firfoxVersions.add("41.0b9");
        firfoxVersions.add("42.0-funnelcake58");
        firfoxVersions.add("42.0-funnelcake59");
        firfoxVersions.add("42.0-funnelcake60");
        firfoxVersions.add("42.0-funnelcake61");
        firfoxVersions.add("42.0-funnelcake62");
        firfoxVersions.add("42.0");
        firfoxVersions.add("42.0b1");
        firfoxVersions.add("42.0b2");
        firfoxVersions.add("42.0b3");
        firfoxVersions.add("42.0b4");
        firfoxVersions.add("42.0b5");
        firfoxVersions.add("42.0b6");
        firfoxVersions.add("42.0b7");
        firfoxVersions.add("42.0b8");
        firfoxVersions.add("42.0b9");
        firfoxVersions.add("43.0.1");
        firfoxVersions.add("43.0.2");
        firfoxVersions.add("43.0.3");
        firfoxVersions.add("43.0.4");
        firfoxVersions.add("43.0");
        firfoxVersions.add("43.0b1");
        firfoxVersions.add("43.0b2");
        firfoxVersions.add("43.0b3");
        firfoxVersions.add("43.0b4");
        firfoxVersions.add("43.0b5");
        firfoxVersions.add("43.0b6");
        firfoxVersions.add("43.0b7");
        firfoxVersions.add("43.0b8");
        firfoxVersions.add("43.0b9");
        firfoxVersions.add("44.0.1");
        firfoxVersions.add("44.0.2");
        firfoxVersions.add("44.0");
        firfoxVersions.add("44.0b1");
        firfoxVersions.add("44.0b2");
        firfoxVersions.add("44.0b4");
        firfoxVersions.add("44.0b6");
        firfoxVersions.add("44.0b7");
        firfoxVersions.add("44.0b8");
        firfoxVersions.add("44.0b9");
        firfoxVersions.add("45.0.1");
        firfoxVersions.add("45.0.1esr");
        firfoxVersions.add("45.0.2");
        firfoxVersions.add("45.0.2esr");
        firfoxVersions.add("45.0");
        firfoxVersions.add("45.0b1");
        firfoxVersions.add("45.0b10");
        firfoxVersions.add("45.0b2");
        firfoxVersions.add("45.0b3");
        firfoxVersions.add("45.0b4");
        firfoxVersions.add("45.0b5");
        firfoxVersions.add("45.0b6");
        firfoxVersions.add("45.0b7");
        firfoxVersions.add("45.0b8");
        firfoxVersions.add("45.0b9");
        firfoxVersions.add("45.0esr");
        firfoxVersions.add("45.1.0esr");
        firfoxVersions.add("45.1.1esr");
        firfoxVersions.add("45.2.0esr");
        firfoxVersions.add("45.3.0esr");
        firfoxVersions.add("45.4.0esr");
        firfoxVersions.add("45.5.0esr");
        firfoxVersions.add("45.5.1esr");
        firfoxVersions.add("45.6.0esr");
        firfoxVersions.add("46.0.1-funnelcake75");
        firfoxVersions.add("46.0.1-funnelcake76");
        firfoxVersions.add("46.0.1-funnelcake77");
        firfoxVersions.add("46.0.1-funnelcake78");
        firfoxVersions.add("46.0.1-funnelcake79");
        firfoxVersions.add("46.0.1-funnelcake80");
        firfoxVersions.add("46.0.1-funnelcake81");
        firfoxVersions.add("46.0.1-funnelcake82");
        firfoxVersions.add("46.0.1-funnelcake83");
        firfoxVersions.add("46.0.1-funnelcake84");
        firfoxVersions.add("46.0.1");
        firfoxVersions.add("46.0");
        firfoxVersions.add("46.0b1");
        firfoxVersions.add("46.0b10");
        firfoxVersions.add("46.0b11");
        firfoxVersions.add("46.0b2");
        firfoxVersions.add("46.0b4");
        firfoxVersions.add("46.0b5");
        firfoxVersions.add("46.0b6");
        firfoxVersions.add("46.0b7");
        firfoxVersions.add("46.0b8");
        firfoxVersions.add("46.0b9");
        firfoxVersions.add("47.0-funnelcake63");
        firfoxVersions.add("47.0-funnelcake66");
        firfoxVersions.add("47.0-funnelcake67");
        firfoxVersions.add("47.0-funnelcake68");
        firfoxVersions.add("47.0.1-funnelcake85");
        firfoxVersions.add("47.0.1-funnelcake86-v12-1");
        firfoxVersions.add("47.0.1-funnelcake86-v12");
        firfoxVersions.add("47.0.1-funnelcake86");
        firfoxVersions.add("47.0.1");
        firfoxVersions.add("47.0.2");
        firfoxVersions.add("47.0");
        firfoxVersions.add("47.0b1");
        firfoxVersions.add("47.0b2");
        firfoxVersions.add("47.0b3");
        firfoxVersions.add("47.0b4");
        firfoxVersions.add("47.0b5");
        firfoxVersions.add("47.0b6");
        firfoxVersions.add("47.0b7");
        firfoxVersions.add("47.0b8");
        firfoxVersions.add("47.0b9");
        firfoxVersions.add("48.0.1");
        firfoxVersions.add("48.0.2");
        firfoxVersions.add("48.0");
        firfoxVersions.add("48.0b1");
        firfoxVersions.add("48.0b10");
        firfoxVersions.add("48.0b2");
        firfoxVersions.add("48.0b3");
        firfoxVersions.add("48.0b4");
        firfoxVersions.add("48.0b5");
        firfoxVersions.add("48.0b6");
        firfoxVersions.add("48.0b7");
        firfoxVersions.add("48.0b9");
        firfoxVersions.add("49.0.1-funnelcake87");
        firfoxVersions.add("49.0.1-funnelcake88");
        firfoxVersions.add("49.0.1-funnelcake89");
        firfoxVersions.add("49.0.1-funnelcake90");
        firfoxVersions.add("49.0.1");
        firfoxVersions.add("49.0.2");
        firfoxVersions.add("49.0");
        firfoxVersions.add("49.0b1");
        firfoxVersions.add("49.0b10");
        firfoxVersions.add("49.0b2");
        firfoxVersions.add("49.0b3");
        firfoxVersions.add("49.0b4");
        firfoxVersions.add("49.0b5");
        firfoxVersions.add("49.0b6");
        firfoxVersions.add("49.0b7");
        firfoxVersions.add("49.0b8");
        firfoxVersions.add("49.0b9");
        firfoxVersions.add("5.0.1");
        firfoxVersions.add("5.0");
        firfoxVersions.add("5.0b1");
        firfoxVersions.add("5.0b2");
        firfoxVersions.add("5.0b3");
        firfoxVersions.add("5.0b5");
        firfoxVersions.add("5.0b6");
        firfoxVersions.add("5.0b7");
        firfoxVersions.add("50.0-funnelcake93");
        firfoxVersions.add("50.0-funnelcake94");
        firfoxVersions.add("50.0.1-funnelcake91");
        firfoxVersions.add("50.0.1-funnelcake92");
        firfoxVersions.add("50.0.1-funnelcake93");
        firfoxVersions.add("50.0.1-funnelcake94");
        firfoxVersions.add("50.0.1");
        firfoxVersions.add("50.0.2-funnelcake91");
        firfoxVersions.add("50.0.2-funnelcake92");
        firfoxVersions.add("50.0.2-funnelcake93");
        firfoxVersions.add("50.0.2-funnelcake94");
        firfoxVersions.add("50.0.2");
        firfoxVersions.add("50.0");
        firfoxVersions.add("50.0b1");
        firfoxVersions.add("50.0b10");
        firfoxVersions.add("50.0b11");
        firfoxVersions.add("50.0b2");
        firfoxVersions.add("50.0b3");
        firfoxVersions.add("50.0b4");
        firfoxVersions.add("50.0b5");
        firfoxVersions.add("50.0b6");
        firfoxVersions.add("50.0b7");
        firfoxVersions.add("50.0b8");
        firfoxVersions.add("50.0b9");
        firfoxVersions.add("50.1.0-funnelcake95");
        firfoxVersions.add("50.1.0-funnelcake96");
        firfoxVersions.add("50.1.0-funnelcake97");
        firfoxVersions.add("50.1.0");
        firfoxVersions.add("51.0b1");
        firfoxVersions.add("51.0b10");
        firfoxVersions.add("51.0b11");
        firfoxVersions.add("51.0b12");
        firfoxVersions.add("51.0b13");
        firfoxVersions.add("51.0b14");
        firfoxVersions.add("51.0b2");
        firfoxVersions.add("51.0b3");
        firfoxVersions.add("51.0b4");
        firfoxVersions.add("51.0b5");
        firfoxVersions.add("51.0b6");
        firfoxVersions.add("51.0b7");
        firfoxVersions.add("51.0b8");
        firfoxVersions.add("51.0b9");
    }

}
