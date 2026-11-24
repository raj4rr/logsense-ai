package com.logsense.analysis;
import java.util.Locale;
public final class ErrorFingerprint {
    private ErrorFingerprint() {}
    public static String of(String message) {
        if (message == null) return "";
        return message.toLowerCase(Locale.ROOT)
                .replaceAll("[0-9a-f]{8}-[0-9a-f-]{27,}", "<uuid>")
                .replaceAll("\\b\\d+\\b", "<n>")
                .replaceAll("0x[0-9a-f]+", "<hex>")
                .replaceAll("\\s+", " ").trim();
    }
}
