package com.smipl.lcrecon.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class JsonUtil {
    private static final Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create();

    public static String toJson(Object obj) {
        return gson.toJson(obj);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return gson.fromJson(json, clazz);
    }

    public static Map<String, String> jsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new LinkedHashMap<>();
        }
        Type type = new TypeToken<LinkedHashMap<String, String>>(){}.getType();
        return gson.fromJson(json, type);
    }

    public static String mapToJson(Map<String, String> map) {
        return gson.toJson(map);
    }

    public static void writeJsonResponse(HttpServletResponse response, Object data) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(toJson(data));
    }

    public static void writeSuccess(HttpServletResponse response, String message) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", message);
        writeJsonResponse(response, result);
    }

    public static void writeSuccess(HttpServletResponse response, String message, Map<String, Object> data) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", message);
        result.putAll(data);
        writeJsonResponse(response, result);
    }

    /**
     * Read a numeric id from a parsed JSON body.
     *
     * JavaScript is inconsistent about this: some pages send {@code id: 5} and others send the
     * value straight out of an HTML attribute as {@code id: "5"}. Casting blindly to Number blows
     * up with a ClassCastException on the string form, so both are accepted here.
     *
     * @throws IllegalArgumentException when the value is missing or not a number - the servlets
     *         translate that into a readable message rather than a generic server error.
     */
    public static long asLong(Object value, String fieldName) {
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            String s = ((String) value).trim();
            if (!s.isEmpty()) {
                try {
                    return (long) Double.parseDouble(s);
                } catch (NumberFormatException ignored) {
                    // fall through to the error below
                }
            }
        }
        throw new IllegalArgumentException("'" + fieldName + "' is missing or is not a valid number.");
    }

    /** Lenient int read: accepts numbers and numeric strings, falling back to defaultValue. */
    public static int asInt(Object value, int defaultValue) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String) {
            String s = ((String) value).trim();
            if (!s.isEmpty()) {
                try {
                    return (int) Double.parseDouble(s);
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
        }
        return defaultValue;
    }

    /** Lenient boolean read: accepts true/false and the strings "true"/"false"/"on"/"1". */
    public static boolean asBoolean(Object value, boolean defaultValue) {
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof Number) return ((Number) value).intValue() != 0;
        if (value instanceof String) {
            String s = ((String) value).trim();
            if (s.equalsIgnoreCase("true") || s.equals("1") || s.equalsIgnoreCase("on")) return true;
            if (s.equalsIgnoreCase("false") || s.equals("0") || s.equalsIgnoreCase("off")) return false;
        }
        return defaultValue;
    }

    public static void writeError(HttpServletResponse response, String message) throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("message", message);
        writeJsonResponse(response, result);
    }

    /**
     * Report an unexpected server-side failure. The exception detail stays in the log (it can
     * contain SQL and internal paths) while the user is given a short reference code, so a report
     * of "it shows an error" can be traced to the exact stack trace with a single log search.
     */
    public static void writeServerError(HttpServletResponse response, org.slf4j.Logger logger,
                                        String context, Exception e) throws IOException {
        String ref = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        logger.error("{} failed [ref={}]", context, ref, e);
        writeError(response, "An error occurred while processing your request. (Ref: " + ref + ")");
    }
}
