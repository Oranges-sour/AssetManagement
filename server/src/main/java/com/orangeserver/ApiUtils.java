package com.orangeserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * api 需要用到的一些工具函数
 */
public final class ApiUtils {
    public static final Pattern STRING_FIELD =
            Pattern.compile("\"%s\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
    public static final Pattern NULL_FIELD =
            Pattern.compile("\"%s\"\\s*:\\s*null", Pattern.CASE_INSENSITIVE);
    public static final Pattern LONG_FIELD =
            Pattern.compile("\"%s\"\\s*:\\s*(\\d+)", Pattern.DOTALL);
    public static final Pattern DECIMAL_FIELD =
            Pattern.compile("\"%s\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)", Pattern.DOTALL);

    private ApiUtils() {
    }

    public static String readBody(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    public static String extractString(String json, String key, Pattern stringFieldPattern) {
        Pattern pattern = Pattern.compile(String.format(stringFieldPattern.pattern(), Pattern.quote(key)),
                stringFieldPattern.flags());
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }
        return null;
    }

    public static String extractNullableString(String json, String key, Pattern stringFieldPattern,
                                               Pattern nullFieldPattern) {
        Pattern nullPattern = Pattern.compile(String.format(nullFieldPattern.pattern(), Pattern.quote(key)),
                nullFieldPattern.flags());
        if (nullPattern.matcher(json).find()) {
            return null;
        }
        return extractString(json, key, stringFieldPattern);
    }

    public static Long extractLong(String body, String key) {
        String value = extractString(body, key, LONG_FIELD);
        if (isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Long extractNullableLong(String body, String key) {
        Pattern nullPattern = Pattern.compile(String.format(NULL_FIELD.pattern(), Pattern.quote(key)),
                NULL_FIELD.flags());
        if (nullPattern.matcher(body).find()) {
            return null;
        }
        return extractLong(body, key);
    }

    public static BigDecimal extractDecimal(String body, String key) {
        String value = extractString(body, key, DECIMAL_FIELD);
        if (isBlank(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Long parseId(String pathInfo) {
        if (pathInfo == null) {
            return null;
        }
        String value = pathInfo.trim();
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        if (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isEmpty()) {
            return null;
        }
        if (!value.matches("\\d+")) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int parseInt(String value, int defaultValue) {
        if (isBlank(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static Long parseLongParam(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static Integer parseIntParam(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static void writeJson(HttpServletResponse resp, int code, String msg, String data)
            throws IOException {
        resp.getWriter().println("{ \"code\": " + code + ", \"msg\": \"" + escapeJson(msg)
                + "\", \"data\": " + data + " }");
    }

    public static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public static String unescapeJson(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
