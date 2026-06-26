package com.kaptaitourist.kaptaitourist.core.util;

/** Masking helpers for displaying sensitive fields in a profile (show only the tail). */
public final class MaskUtil {

    private MaskUtil() {
    }

    /** Masks all but the last {@code visible} characters with '*'. Short values are returned as-is. */
    public static String maskKeepLast(String value, int visible) {
        if (value == null) return null;
        if (value.length() <= visible) return value;
        int maskCount = value.length() - visible;
        return "*".repeat(maskCount) + value.substring(maskCount);
    }

    /** Masks the mobile, leaving the last 4 digits visible, e.g. "**********0000". */
    public static String maskMobile(String mobile) {
        return maskKeepLast(mobile, 4);
    }

    /**
     * Masks the local part of an email, leaving the last 4 chars before '@' visible and the
     * domain intact, e.g. "rafiyad@example.com" → "***iyad@example.com".
     */
    public static String maskEmail(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        if (at <= 0) return maskKeepLast(email, 4);
        String local = email.substring(0, at);
        String domain = email.substring(at); // includes '@'
        return maskKeepLast(local, 4) + domain;
    }
}
