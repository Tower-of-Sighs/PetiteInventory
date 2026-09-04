package com.sighs.petiteinventory.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Ensures generated TACZ examples stay present in existing config files. */
public final class TaczConfigGuard {
    private static final String TACZ_ITEM_EXAMPLE =
            "  {\n" +
            "    \"match\": [\"tacz:modern_kinetic_gun{GunId:\\\"your_gun_id\\\"}\"],\n" +
            "    \"result\": \"2*1\"\n" +
            "  }";

    private static final String TACZ_COLOR_EXAMPLE =
            "  {\n" +
            "    \"match\": [\"tacz:modern_kinetic_gun{GunId:\\\"your_gun_id\\\"}\"],\n" +
            "    \"theme\": \"red\"\n" +
            "  }";

    private TaczConfigGuard() {
    }

    public static void ensureItemExample(Path file) {
        ensureExample(file, TACZ_ITEM_EXAMPLE);
    }

    public static void ensureColorExample(Path file) {
        ensureExample(file, TACZ_COLOR_EXAMPLE);
    }

    private static void ensureExample(Path file, String exampleJson) {
        if (file == null || !Files.exists(file)) {
            return;
        }

        try {
            String raw = Files.readString(file, StandardCharsets.UTF_8);
            if (raw.contains("tacz:")) {
                return;
            }

            String updated = insertExample(raw, exampleJson);
            if (updated != null) {
                Files.writeString(file, updated, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String insertExample(String raw, String exampleJson) {
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            String inside = trimmed.substring(1, trimmed.length() - 1).trim();
            if (inside.isEmpty()) {
                return "[\n" + exampleJson + "\n]";
            }
        }

        int lastBracket = raw.lastIndexOf(']');
        if (lastBracket < 0) {
            return null;
        }

        String prefix = stripTrailingWhitespace(raw.substring(0, lastBracket));
        return prefix + ",\n" + exampleJson + "\n" + raw.substring(lastBracket);
    }

    private static String stripTrailingWhitespace(String value) {
        int end = value.length();
        while (end > 0 && Character.isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(0, end);
    }
}
