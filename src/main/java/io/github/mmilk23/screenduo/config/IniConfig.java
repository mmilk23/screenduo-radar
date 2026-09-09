package io.github.mmilk23.screenduo.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

final class IniConfig {

    private final Map<String, String> values;

    private IniConfig(Map<String, String> values) {
        this.values = Map.copyOf(values);
    }

    static IniConfig load(Path path) throws IOException {
        if (!Files.isRegularFile(path)) {
            throw new IOException("Configuration file not found: " + path.toAbsolutePath());
        }

        Map<String, String> values = new LinkedHashMap<>();
        String section = "";
        int lineNumber = 0;

        for (String sourceLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            lineNumber++;
            String line = sourceLine.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                if (section.isEmpty()) {
                    throw invalid(path, lineNumber, "section name is empty");
                }
                continue;
            }

            int separator = line.indexOf('=');
            if (separator < 1) {
                throw invalid(path, lineNumber, "expected key=value");
            }
            String key = line.substring(0, separator).trim();
            String value = unquote(line.substring(separator + 1).trim());
            String qualifiedKey = section.isEmpty() ? key : section + "." + key;
            if (values.putIfAbsent(qualifiedKey, value) != null) {
                throw invalid(path, lineNumber, "duplicate key " + qualifiedKey);
            }
        }
        return new IniConfig(values);
    }

    String required(String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required configuration is missing: " + key);
        }
        return value;
    }

    String optional(String key, String defaultValue) {
        String value = values.get(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private static String unquote(String value) {
        if (value.length() >= 2
                && ((value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"')
                || (value.charAt(0) == '\'' && value.charAt(value.length() - 1) == '\''))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static IOException invalid(Path path, int lineNumber, String reason) {
        return new IOException("Invalid INI file " + path + " at line " + lineNumber + ": " + reason);
    }
}
