package io.github.mmilk23.screenduo.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvReaderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsQuotedCommasQuotesAndNewlines() throws IOException {
        Path file = Files.writeString(
                temporaryDirectory.resolve("sample.csv"),
                "code,name\r\nJBU,\"JetBlue Airways\"\r\nTST,\"Test, \"\"Airline\"\"\"\r\n");
        List<List<String>> rows = new ArrayList<>();

        CsvReader.read(file, rows::add);

        assertEquals(List.of(
                List.of("code", "name"),
                List.of("JBU", "JetBlue Airways"),
                List.of("TST", "Test, \"Airline\"")), rows);
    }
}
