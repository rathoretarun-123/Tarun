package com.tarun.kahani.core;

import java.io.ByteArrayOutputStream;

/** Base64 without a platform class (the core runs on the phone and on the desktop alike). */
public final class Base64 {
    private Base64() {}

    private static final String T = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    public static String encode(byte[] data) {
        StringBuilder b = new StringBuilder((data.length + 2) / 3 * 4);
        for (int i = 0; i < data.length; i += 3) {
            int n = (data[i] & 255) << 16 | (i + 1 < data.length ? (data[i + 1] & 255) << 8 : 0) | (i + 2 < data.length ? data[i + 2] & 255 : 0);
            b.append(T.charAt(n >> 18 & 63)).append(T.charAt(n >> 12 & 63));
            b.append(i + 1 < data.length ? T.charAt(n >> 6 & 63) : '=');
            b.append(i + 2 < data.length ? T.charAt(n & 63) : '=');
        }
        return b.toString();
    }

    /** Decodes standard or URL-safe base64, ignoring whitespace and padding. */
    public static byte[] decode(String s) {
        ByteArrayOutputStream o = new ByteArrayOutputStream(s.length() * 3 / 4 + 3);
        int acc = 0, bits = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int v;
            if (c >= 'A' && c <= 'Z') v = c - 'A';
            else if (c >= 'a' && c <= 'z') v = c - 'a' + 26;
            else if (c >= '0' && c <= '9') v = c - '0' + 52;
            else if (c == '+' || c == '-') v = 62;
            else if (c == '/' || c == '_') v = 63;
            else continue;                       // '=', newlines, spaces
            acc = acc << 6 | v;
            bits += 6;
            if (bits >= 8) { bits -= 8; o.write(acc >> bits & 255); }
        }
        return o.toByteArray();
    }
}
