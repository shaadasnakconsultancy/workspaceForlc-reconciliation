package com.smipl.lcrecon.util;

/**
 * Lightweight server-side input validation (defense-in-depth against Stored XSS).
 * Output encoding at render time is the primary control; this rejects HTML tag
 * characters in plain-text fields before they are stored.
 */
public final class InputValidator {

    private InputValidator() {}

    /** True if the value contains characters used to inject HTML/script tags. */
    public static boolean hasHtmlTagChars(String s) {
        return s != null && (s.indexOf('<') >= 0 || s.indexOf('>') >= 0);
    }

    /**
     * Rejects a plain-text field that contains angle brackets.
     * @throws IllegalArgumentException if the value contains {@code <} or {@code >}
     */
    public static void rejectHtml(String fieldLabel, String value) {
        if (hasHtmlTagChars(value)) {
            throw new IllegalArgumentException(fieldLabel + " must not contain '<' or '>' characters.");
        }
    }

    /**
     * Strip characters used to break out of HTML/attribute/URL contexts.
     * Used to sanitize reflected request parameters (e.g. search filters) before they
     * are echoed back into the page, defending against Reflected XSS.
     */
    public static String stripUnsafe(String s) {
        if (s == null) return null;
        return s.replaceAll("[<>\"'`]", "").trim();
    }

    /**
     * Return the value only if it is a plain ISO date (yyyy-MM-dd); otherwise empty string.
     * Used for reflected date filter parameters.
     */
    public static String safeDate(String s) {
        if (s == null) return "";
        s = s.trim();
        return s.matches("\\d{4}-\\d{2}-\\d{2}") ? s : "";
    }
}
