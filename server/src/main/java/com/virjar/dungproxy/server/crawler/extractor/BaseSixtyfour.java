package com.virjar.dungproxy.server.crawler.extractor;

// base64编解码
public class BaseSixtyfour {
    private static final char[] base64std = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
            .toCharArray();
    private static final int[] base64res = new int[128];

    static {
        for (int i = 0; i < 64; i++) {
            // int temp= base64std[i] & 0xff;
            base64res[base64std[i]] = i;
        }
    }

    // base64编码
    public static String encode(byte[] data) {
        int indata = 0;
        char[] dest = new char[(data.length + 2) / 3 * 4];
        int n = 3 * (data.length / 3);
        int i, j;
        int[] a = new int[3];
        for (i = 0, j = 0; i < n; i += 3) {
            a[0] = data[i] & 0xff;
            a[1] = data[i + 1] & 0xff;
            a[2] = data[i + 2] & 0xff;
            indata = (a[0] << 16) + (a[1] << 8) + a[2];
            dest[j++] = base64std[(indata >> 18)];
            dest[j++] = base64std[((indata >> 12) & 0x3F)];
            dest[j++] = base64std[((indata >> 6) & 0x3F)];
            dest[j++] = base64std[(indata & 0x3F)];
        }
        if (data.length % 3 == 1) {
            indata = data[i] & 0xff;
            dest[j++] = base64std[indata >> 2];
            dest[j++] = base64std[(indata << 4) & 0x3f];
            dest[j++] = '=';
            dest[j++] = '=';
        } else if (data.length % 3 == 2) {
            indata = ((data[i] & 0xff) << 8) + (data[i + 1] & 0xff);
            dest[j++] = base64std[indata >> 10];
            dest[j++] = base64std[(indata >> 4) & 0x3f];
            dest[j++] = base64std[(indata << 2) & 0x3f];
            dest[j++] = '=';
        }
        return String.valueOf(dest);
    }

    // base64解码
    public static byte[] decode(String indata) {
        byte[] out;
        if (indata.contains("=")) {
            int x = indata.length() - indata.indexOf('=');
            out = new byte[indata.length() * 3 / 4 - x];
        } else {
            out = new byte[indata.length() * 3 / 4];
        }

        int i, j, n;
        int temp;
        n = indata.length() - 4;
        for (i = 0, j = 0; i < n; i += 4) {
            temp = (base64res[indata.charAt(i)] << 18) + (base64res[indata.charAt(i + 1)] << 12)
                    + (base64res[indata.charAt(i + 2)] << 6) + (base64res[indata.charAt(i + 3)]);
            out[j++] = (byte) (temp >> 16);
            out[j++] = (byte) ((temp >> 8) & 0xff);
            out[j++] = (byte) (temp & 0xff);
        }
        if (indata.charAt(i + 2) == '=') {
            out[j++] = (byte) ((base64res[indata.charAt(i)] << 2) + (base64res[indata.charAt(i + 1)] >>> 4));
        } else if (indata.charAt(i + 3) == '=') {
            temp = (base64res[indata.charAt(i)] << 10) + (base64res[indata.charAt(i + 1)] << 4)
                    + (base64res[indata.charAt(i + 2)] >>> 2);
            out[j++] = (byte) (temp >> 8);
            out[j++] = (byte) (temp & 0xff);
        } else {
            temp = (base64res[indata.charAt(i)] << 18) + (base64res[indata.charAt(i + 1)] << 12)
                    + (base64res[indata.charAt(i + 2)] << 6) + (base64res[indata.charAt(i + 3)]);
            out[j++] = (byte) (temp >> 16);
            out[j++] = (byte) ((temp >> 8) & 0xff);
            out[j++] = (byte) (temp & 0xff);
        }
        return out;
    }
}
