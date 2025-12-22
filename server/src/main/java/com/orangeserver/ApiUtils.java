package com.orangeserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * api 需要用到的一些工具函数
 */
public final class ApiUtils {
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
