/*
 * This is a private project. All rights reserved.
 */
package com.storytellerf.litert.compat;

/** Preserves JSON values across JNI's Modified UTF-8 transport. */
public final class JniJsonEscaper {
    private JniJsonEscaper() {}

    public static String escape(String json) {
        StringBuilder escaped = null;
        for (int index = 0; index < json.length(); index++) {
            char current = json.charAt(index);
            if (Character.isHighSurrogate(current)) {
                if (index + 1 >= json.length() || !Character.isLowSurrogate(json.charAt(index + 1))) {
                    throw new IllegalArgumentException("Unpaired surrogate in LiteRT JSON request");
                }
                if (escaped == null) {
                    escaped = new StringBuilder(json.length() + 12);
                    escaped.append(json, 0, index);
                }
                appendEscape(escaped, current);
                appendEscape(escaped, json.charAt(++index));
            } else if (Character.isLowSurrogate(current)) {
                throw new IllegalArgumentException("Unpaired surrogate in LiteRT JSON request");
            } else if (escaped != null) {
                escaped.append(current);
            }
        }
        return escaped == null ? json : escaped.toString();
    }

    private static void appendEscape(StringBuilder target, char value) {
        target.append('\\').append('u');
        for (int shift = 12; shift >= 0; shift -= 4) {
            target.append(Character.forDigit((value >> shift) & 15, 16));
        }
    }
}
