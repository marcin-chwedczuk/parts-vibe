package app.partsvibe.shared.utils;

public final class StringUtils {
    private StringUtils() {}

    public static String truncate(String value, int maxLength) {
        if (value == null || maxLength < 0 || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    public static String normalizeToEmpty(String value) {
        return value == null ? "" : value;
    }

    public static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
