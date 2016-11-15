
package com.virjar;


import com.ning.http.client.AsyncCompletionHandler;
import com.ning.http.client.Response;
import com.virjar.client.proxyclient.ProxyClient;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.net.URLEncoder;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Description: ClientTest
 *
 * @author lingtong.fu
 * @version 2016-09-04 23:58
 */


public class ClientTest {

    public static void main(String[] args) {
        ClientTest test = new ClientTest();
        try {
            Phone phone = test.crawl("13693249012").get(MAX_CRAWL_TIMEOUT, TimeUnit.MILLISECONDS);
            System.out.println(phone.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static final long MAX_CRAWL_TIMEOUT = 60000;

    private static final String url = "http://www.baidu.com/s?rsv_bp=0&inputT=4659&wd=%e5%bd%92%e5%b1%9e%e5%9c%b0+";

    private static final Pattern pattern = Pattern
            .compile("<div\\s*class=\"op_mobilephone_r\">\\s*<span>[^>]*>[^>]*>([^&]*)&nbsp;([^&]*)&nbsp;([^&]*)&nbsp;([^<]*)<");

    public Future<Phone> crawl(String phone) throws IOException {
        ProxyClient proxyClient = new ProxyClient();
        return proxyClient.get(url + URLEncoder.encode(phone, "utf-8"), new AsyncCompletionHandler<Phone>() {
            @Override
            public Phone onCompleted(Response response) throws Exception {
                if (response.getStatusCode() == 200) {
                    return parsePhone(response.getResponseBody());
                }
                return null;
            }
        });
    }

    public static Phone parsePhone(String data) {
        Matcher m = pattern.matcher(data);
        if (!m.find()) {
            return null;
        }
        Phone phone = new Phone();
        phone.setProvince(m.group(1));
        phone.setCity(m.group(2));
        String type = m.group(4);
        if (StringUtils.isEmpty(phone.getProvince())) {
            phone.setProvince(phone.getCity());
        }
        if (StringUtils.contains(type, "中国移动")) {
            phone.setMmo((short) 1);
        } else if (StringUtils.contains(type, "中国联通")) {
            phone.setMmo((short) 2);
        } else if (StringUtils.contains(type, "中国电信")) {
            phone.setMmo((short) 3);
        } else {
            phone.setMmo((short) 0);
        }
        return phone;
    }

    private static class Phone implements Serializable {

        private static final long serialVersionUID = -6732131032651082674L;

        private String province;
        private String city;
        private short mmo;

        public Phone() {
        }

        public Phone(String province, String city, short mmo) {
            this.province = province;
            this.city = city;
            this.mmo = mmo;
        }

        public String getProvince() {
            return province;
        }

        public void setProvince(String province) {
            this.province = province;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public short getMmo() {
            return mmo;
        }

        public void setMmo(short mmo) {
            this.mmo = mmo;
        }

        @Override
        public String toString() {
            return "Phone [" +
                    "province=" + province +
                    ", city=" + city +
                    ", mmo=" + mmo +
                    "]";
        }
    }
}
