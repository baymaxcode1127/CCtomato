package pomodoro.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple JSON utility — no external dependencies.
 * Supports objects, strings, numbers, booleans, arrays, and nested objects.
 */
public class JsonUtil {

    // ==================== Serialization ====================

    public static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        writeObject(sb, map, 0);
        return sb.toString();
    }

    public static String toJsonArray(List<Map<String, Object>> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            writeObject(sb, list.get(i), 2);
            if (i < list.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    private static void writeObject(StringBuilder sb, Map<String, Object> map, int indent) {
        String pad = "  ".repeat(indent);
        String innerPad = "  ".repeat(indent + 1);
        sb.append(pad).append("{\n");
        int i = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            sb.append(innerPad).append("\"").append(escape(entry.getKey())).append("\": ");
            writeValue(sb, entry.getValue(), indent + 1);
            if (i < map.size() - 1) sb.append(",");
            sb.append("\n");
            i++;
        }
        sb.append(pad).append("}");
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder sb, Object value, int indent) {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof String) {
            sb.append("\"").append(escape((String) value)).append("\"");
        } else if (value instanceof Integer) {
            sb.append(((Integer) value).intValue());
        } else if (value instanceof Long) {
            sb.append(((Long) value).longValue());
        } else if (value instanceof Double) {
            sb.append(((Double) value).doubleValue());
        } else if (value instanceof Boolean) {
            sb.append(((Boolean) value).booleanValue());
        } else if (value instanceof Map) {
            writeObject(sb, (Map<String, Object>) value, indent);
        } else if (value instanceof List) {
            writeArray(sb, (List<Object>) value, indent);
        } else {
            sb.append("\"").append(escape(value.toString())).append("\"");
        }
    }

    private static void writeArray(StringBuilder sb, List<Object> list, int indent) {
        String pad = "  ".repeat(indent);
        sb.append("[\n");
        for (int i = 0; i < list.size(); i++) {
            sb.append(pad).append("  ");
            writeValue(sb, list.get(i), indent + 1);
            if (i < list.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append(pad).append("]");
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // ==================== Deserialization ====================

    public static Map<String, Object> parseObject(String json) {
        ParseResult result = parseValue(json.trim(), 0);
        if (result.value instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) result.value;
            return map;
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> parseArray(String json) {
        List<Map<String, Object>> list = new ArrayList<>();
        String trimmed = json.trim();
        if (!trimmed.startsWith("[")) return list;

        ParseResult result = parseValue(trimmed, 0);
        if (result.value instanceof List) {
            List<Object> raw = (List<Object>) result.value;
            for (Object obj : raw) {
                if (obj instanceof Map) {
                    list.add((Map<String, Object>) obj);
                }
            }
        }
        return list;
    }

    private static ParseResult parseValue(String json, int pos) {
        pos = skipWhitespace(json, pos);
        if (pos >= json.length()) return new ParseResult(null, pos);

        char c = json.charAt(pos);
        if (c == '"') return parseString(json, pos);
        if (c == '{') return parseObjectInner(json, pos);
        if (c == '[') return parseArrayInner(json, pos);
        if (c == 't' || c == 'f') return parseBoolean(json, pos);
        if (c == 'n') return parseNull(json, pos);
        return parseNumber(json, pos);
    }

    private static ParseResult parseObjectInner(String json, int pos) {
        Map<String, Object> map = new LinkedHashMap<>();
        pos++; // skip '{'
        while (pos < json.length()) {
            pos = skipWhitespace(json, pos);
            if (pos >= json.length()) break;
            if (json.charAt(pos) == '}') {
                pos++;
                break;
            }
            // parse key
            ParseResult keyResult = parseString(json, pos);
            String key = (String) keyResult.value;
            pos = skipWhitespace(json, keyResult.pos);
            if (json.charAt(pos) == ':') pos++;
            pos = skipWhitespace(json, pos);
            // parse value
            ParseResult valueResult = parseValue(json, pos);
            map.put(key, valueResult.value);
            pos = skipWhitespace(json, valueResult.pos);
            if (pos < json.length() && json.charAt(pos) == ',') pos++;
        }
        return new ParseResult(map, pos);
    }

    private static ParseResult parseArrayInner(String json, int pos) {
        List<Object> list = new ArrayList<>();
        pos++; // skip '['
        while (pos < json.length()) {
            pos = skipWhitespace(json, pos);
            if (pos >= json.length()) break;
            if (json.charAt(pos) == ']') {
                pos++;
                break;
            }
            ParseResult valueResult = parseValue(json, pos);
            list.add(valueResult.value);
            pos = skipWhitespace(json, valueResult.pos);
            if (pos < json.length() && json.charAt(pos) == ',') pos++;
        }
        return new ParseResult(list, pos);
    }

    private static ParseResult parseString(String json, int pos) {
        pos++; // skip opening '"'
        StringBuilder sb = new StringBuilder();
        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (c == '\\') {
                pos++;
                if (pos < json.length()) {
                    char escaped = json.charAt(pos);
                    switch (escaped) {
                        case '"': sb.append('"'); break;
                        case '\\': sb.append('\\'); break;
                        case '/': sb.append('/'); break;
                        case 'n': sb.append('\n'); break;
                        case 'r': sb.append('\r'); break;
                        case 't': sb.append('\t'); break;
                        default: sb.append(escaped); break;
                    }
                }
            } else if (c == '"') {
                pos++;
                break;
            } else {
                sb.append(c);
            }
            pos++;
        }
        return new ParseResult(sb.toString(), pos);
    }

    private static ParseResult parseNumber(String json, int pos) {
        StringBuilder sb = new StringBuilder();
        boolean isDouble = false;
        while (pos < json.length()) {
            char c = json.charAt(pos);
            if (c == '-' || Character.isDigit(c)) {
                sb.append(c);
            } else if (c == '.' || c == 'e' || c == 'E') {
                sb.append(c);
                isDouble = true;
            } else {
                break;
            }
            pos++;
        }
        String numStr = sb.toString();
        if (isDouble) {
            return new ParseResult(Double.parseDouble(numStr), pos);
        } else {
            try {
                return new ParseResult(Integer.parseInt(numStr), pos);
            } catch (NumberFormatException e) {
                return new ParseResult(Long.parseLong(numStr), pos);
            }
        }
    }

    private static ParseResult parseBoolean(String json, int pos) {
        if (json.startsWith("true", pos)) {
            return new ParseResult(Boolean.TRUE, pos + 4);
        } else {
            return new ParseResult(Boolean.FALSE, pos + 5);
        }
    }

    private static ParseResult parseNull(String json, int pos) {
        return new ParseResult(null, pos + 4);
    }

    private static int skipWhitespace(String json, int pos) {
        while (pos < json.length() && Character.isWhitespace(json.charAt(pos))) {
            pos++;
        }
        return pos;
    }

    // ==================== Helper Methods ====================

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getArray(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof List) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : (List<Object>) val) {
                if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> getObject(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Map) {
            return (Map<String, Object>) val;
        }
        return new LinkedHashMap<>();
    }

    public static String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    public static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object val = map.get(key);
        return val != null ? val.toString() : defaultValue;
    }

    public static int getInt(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (NumberFormatException e) { }
        }
        return 0;
    }

    public static int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object val = map.get(key);
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) {
            try { return Integer.parseInt((String) val); } catch (NumberFormatException e) { }
        }
        return defaultValue;
    }

    public static boolean getBoolean(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) return Boolean.parseBoolean((String) val);
        return false;
    }

    public static Map<String, Object> toMap(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    // ==================== Parse Result ====================

    private static class ParseResult {
        final Object value;
        final int pos;

        ParseResult(Object value, int pos) {
            this.value = value;
            this.pos = pos;
        }
    }
}
