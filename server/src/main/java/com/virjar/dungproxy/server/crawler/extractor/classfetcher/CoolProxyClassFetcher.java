package com.virjar.dungproxy.server.crawler.extractor.classfetcher;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.htmlcleaner.TagNode;

import com.virjar.dungproxy.server.crawler.extractor.ClassFetcher;

/**
 * Created by virjar on 16/11/27.
 */
public class CoolProxyClassFetcher implements ClassFetcher {
    private static Base64 base64 = new Base64();

    @Override
    public String fetcher(TagNode tagnode, int type) {
        String text = StringUtils.substringBetween(tagnode.getText().toString().trim(), "str_rot13(\"", "\")");
        if(StringUtils.isEmpty(text)){
            return "";
        }
        return new String(base64.decode(decodeRot13(text)));
    }

    private static String decodeRot13(String input) {
        char[] chars = input.toCharArray();
        StringBuilder sb = new StringBuilder(input.length());
        for (Character ch : chars) {
            if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                sb.append((char) (Character.toLowerCase(ch) < 'n' ? ch + 13 : ch - 13));
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        System.out.println(new String(base64.decode(decodeRot13("BQxhZGLmYwVlAP42ZD=="))));
    }
}
