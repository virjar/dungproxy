package com.virjar.dungproxy.server.crawler.extractor;

public class Decoder {

    public static String decode(String str, String decoder) {
        if (decoder == null)
            return str;
        try {
            if (decoder.toLowerCase().equals("base64")) {
                return new String(BaseSixtyfour.decode(str));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }
}
