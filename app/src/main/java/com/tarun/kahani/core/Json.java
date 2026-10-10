package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tiny JSON reader/writer (no external libraries). Objects -> LinkedHashMap, arrays -> ArrayList. */
public final class Json {
    private final String s;
    private int i;

    private Json(String s) { this.s = s; }

    public static Object parse(String text) {
        if (text == null) return null;
        Json j = new Json(text);
        j.ws();
        return j.value();
    }

    /** Parses the first JSON object/array found inside free text (LLM answers often wrap JSON in prose or ```). */
    public static Object parseLoose(String text) {
        if (text == null) return null;
        int a = text.indexOf('{'), b = text.indexOf('[');
        int start = a < 0 ? b : b < 0 ? a : Math.min(a, b);
        if (start < 0) return null;
        try {
            Json j = new Json(text);
            j.i = start;
            return j.value();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }

    private Object value() {
        ws();
        if (i >= s.length()) throw new RuntimeException("end of json");
        char c = s.charAt(i);
        if (c == '{') return obj();
        if (c == '[') return arr();
        if (c == '"') return str();
        if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
        if (s.startsWith("null", i)) { i += 4; return null; }
        return num();
    }

    private Map<String, Object> obj() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        i++;
        ws();
        if (i < s.length() && s.charAt(i) == '}') { i++; return m; }
        while (i < s.length()) {
            ws();
            String k = str();
            ws();
            if (i < s.length() && s.charAt(i) == ':') i++;
            m.put(k, value());
            ws();
            if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
            if (i < s.length() && s.charAt(i) == '}') { i++; break; }
            throw new RuntimeException("bad object at " + i);
        }
        return m;
    }

    private List<Object> arr() {
        List<Object> l = new ArrayList<Object>();
        i++;
        ws();
        if (i < s.length() && s.charAt(i) == ']') { i++; return l; }
        while (i < s.length()) {
            l.add(value());
            ws();
            if (i < s.length() && s.charAt(i) == ',') { i++; continue; }
            if (i < s.length() && s.charAt(i) == ']') { i++; break; }
            throw new RuntimeException("bad array at " + i);
        }
        return l;
    }

    private String str() {
        if (s.charAt(i) != '"') throw new RuntimeException("expected string at " + i);
        i++;
        StringBuilder b = new StringBuilder();
        while (i < s.length()) {
            char c = s.charAt(i++);
            if (c == '"') return b.toString();
            if (c == '\\' && i < s.length()) {
                char e = s.charAt(i++);
                switch (e) {
                    case 'n': b.append('\n'); break;
                    case 't': b.append('\t'); break;
                    case 'r': b.append('\r'); break;
                    case 'b': b.append('\b'); break;
                    case 'f': b.append('\f'); break;
                    case 'u':
                        b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                        break;
                    default: b.append(e);
                }
            } else b.append(c);
        }
        return b.toString();
    }

    private Object num() {
        int a = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        String t = s.substring(a, i);
        if (t.length() == 0) throw new RuntimeException("bad value at " + a);
        try {
            if (t.indexOf('.') < 0 && t.indexOf('e') < 0 && t.indexOf('E') < 0) return Long.parseLong(t);
            return Double.parseDouble(t);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    // ---------------------------------------------------------------- writing

    public static String write(Object o) {
        StringBuilder b = new StringBuilder();
        write(b, o);
        return b.toString();
    }

    @SuppressWarnings("unchecked")
    private static void write(StringBuilder b, Object o) {
        if (o == null) b.append("null");
        else if (o instanceof String) quote(b, (String) o);
        else if (o instanceof Number || o instanceof Boolean) b.append(o);
        else if (o instanceof Map) {
            b.append('{');
            boolean first = true;
            for (Map.Entry<String, Object> e : ((Map<String, Object>) o).entrySet()) {
                if (!first) b.append(',');
                first = false;
                quote(b, e.getKey());
                b.append(':');
                write(b, e.getValue());
            }
            b.append('}');
        } else if (o instanceof List) {
            b.append('[');
            boolean first = true;
            for (Object x : (List<Object>) o) {
                if (!first) b.append(',');
                first = false;
                write(b, x);
            }
            b.append(']');
        } else quote(b, String.valueOf(o));
    }

    public static void quote(StringBuilder b, String s) {
        b.append('"');
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            switch (c) {
                case '"': b.append("\\\""); break;
                case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break;
                case '\r': b.append("\\r"); break;
                case '\t': b.append("\\t"); break;
                default:
                    if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                    else b.append(c);
            }
        }
        b.append('"');
    }

    // ---------------------------------------------------------------- convenient access

    @SuppressWarnings("unchecked")
    public static Map<String, Object> obj(Object o, String key) {
        if (!(o instanceof Map)) return null;
        Object v = ((Map<String, Object>) o).get(key);
        return v instanceof Map ? (Map<String, Object>) v : null;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> arr(Object o, String key) {
        if (!(o instanceof Map)) return new ArrayList<Object>();
        Object v = ((Map<String, Object>) o).get(key);
        return v instanceof List ? (List<Object>) v : new ArrayList<Object>();
    }

    @SuppressWarnings("unchecked")
    public static String str(Object o, String key, String def) {
        if (!(o instanceof Map)) return def;
        Object v = ((Map<String, Object>) o).get(key);
        return v == null ? def : String.valueOf(v);
    }

    @SuppressWarnings("unchecked")
    public static double num(Object o, String key, double def) {
        if (!(o instanceof Map)) return def;
        Object v = ((Map<String, Object>) o).get(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return v == null ? def : Double.parseDouble(String.valueOf(v)); } catch (NumberFormatException e) { return def; }
    }

    @SuppressWarnings("unchecked")
    public static boolean bool(Object o, String key, boolean def) {
        if (!(o instanceof Map)) return def;
        Object v = ((Map<String, Object>) o).get(key);
        if (v instanceof Boolean) return (Boolean) v;
        if (v == null) return def;
        String t = String.valueOf(v).toLowerCase();
        return t.equals("true") || t.equals("yes") || t.equals("1");
    }
}
