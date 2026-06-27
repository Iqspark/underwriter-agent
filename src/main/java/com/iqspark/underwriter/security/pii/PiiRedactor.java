package com.iqspark.underwriter.security.pii;

import java.util.regex.Pattern;

/**
 * Masks personal data so it never lands in logs, error responses, or the audit trail in the clear
 * (doc 11 §4.2). The audit records <em>that</em> a field was used and <em>who</em> accessed it, not
 * raw PII. Defense-in-depth: structured callers should also minimize/tokenize upstream.
 */
public final class PiiRedactor {

    private PiiRedactor() {
    }

    private static final Pattern EMAIL =
            Pattern.compile("\\b[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern CA_POSTAL =
            Pattern.compile("\\b[A-Za-z]\\d[A-Za-z]\\s?\\d[A-Za-z]\\d\\b");
    private static final Pattern LONG_DIGITS =
            Pattern.compile("\\b\\d{6,}\\b");

    /** Redact emails, Canadian postal codes, and long digit runs (account/policy numbers) in free text. */
    public static String redact(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String out = EMAIL.matcher(text).replaceAll("[redacted-email]");
        out = CA_POSTAL.matcher(out).replaceAll("[redacted-postal]");
        out = LONG_DIGITS.matcher(out).replaceAll("[redacted-number]");
        return out;
    }

    /** Mask a person/organization name to initials, e.g. "John Smith" -> "J*** S***". */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        for (String token : name.trim().split("\\s+")) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(token.charAt(0))).append("***");
        }
        return sb.toString();
    }

    /** Mask a street address, keeping only a generic placeholder. */
    public static String maskAddress(String address) {
        return address == null || address.isBlank() ? address : "[redacted-address]";
    }
}
