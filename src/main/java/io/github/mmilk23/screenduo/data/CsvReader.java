package io.github.mmilk23.screenduo.data;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class CsvReader {

    private CsvReader() {
    }

    public static void read(Path path, Consumer<List<String>> rowConsumer) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            List<String> row = new ArrayList<>();
            StringBuilder field = new StringBuilder();
            boolean quoted = false;
            boolean hasContent = false;
            int current;

            while ((current = reader.read()) != -1) {
                char character = (char) current;
                hasContent = true;

                if (quoted) {
                    if (character == '"') {
                        reader.mark(1);
                        int next = reader.read();
                        if (next == '"') {
                            field.append('"');
                        } else {
                            quoted = false;
                            if (next != -1) {
                                reader.reset();
                            }
                        }
                    } else {
                        field.append(character);
                    }
                } else if (character == '"' && field.isEmpty()) {
                    quoted = true;
                } else if (character == ',') {
                    row.add(field.toString());
                    field.setLength(0);
                } else if (character == '\n') {
                    row.add(removeCarriageReturn(field));
                    rowConsumer.accept(List.copyOf(row));
                    row.clear();
                    field.setLength(0);
                    hasContent = false;
                } else {
                    field.append(character);
                }
            }

            if (quoted) {
                throw new IOException("CSV file has an unterminated quoted field: " + path);
            }
            if (hasContent || !row.isEmpty() || !field.isEmpty()) {
                row.add(removeCarriageReturn(field));
                rowConsumer.accept(List.copyOf(row));
            }
        }
    }

    private static String removeCarriageReturn(StringBuilder field) {
        int length = field.length();
        if (length > 0 && field.charAt(length - 1) == '\r') {
            return field.substring(0, length - 1);
        }
        return field.toString();
    }
}
