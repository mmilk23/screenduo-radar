package io.github.mmilk23.screenduo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IniConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsSectionsCommentsQuotesAndDefaults() throws Exception {
        Path file = tempDir.resolve("config.ini");
        Files.writeString(file, """
                # comment
                ; another comment
                root = root-value

                [location]
                city = "Rio de Janeiro"
                country = 'Brazil'
                blank =
                """);

        IniConfig config = IniConfig.load(file);

        assertEquals("root-value", config.required("root"));
        assertEquals("Rio de Janeiro", config.required("location.city"));
        assertEquals("Brazil", config.required("location.country"));
        assertEquals("fallback", config.optional("location.blank", "fallback"));
        assertEquals("fallback", config.optional("location.missing", "fallback"));
    }

    @Test
    void rejectsMissingFileEmptySectionMalformedLineAndDuplicateKeys() throws Exception {
        IOException missing = assertThrows(
                IOException.class,
                () -> IniConfig.load(tempDir.resolve("missing.ini")));
        assertTrue(missing.getMessage().contains("Configuration file not found"));

        assertInvalid("[]\n", "section name is empty");
        assertInvalid("[location]\ninvalid\n", "expected key=value");
        assertInvalid("[location]\ncity=Rio\ncity=Niteroi\n", "duplicate key location.city");
    }

    @Test
    void requiredRejectsMissingOrBlankValues() throws Exception {
        Path file = tempDir.resolve("required.ini");
        Files.writeString(file, "present=value\nblank=\n");
        IniConfig config = IniConfig.load(file);

        assertEquals("value", config.required("present"));
        assertThrows(IllegalArgumentException.class, () -> config.required("missing"));
        assertThrows(IllegalArgumentException.class, () -> config.required("blank"));
    }

    private void assertInvalid(String content, String expectedReason) throws Exception {
        Path file = Files.createTempFile(tempDir, "invalid-", ".ini");
        Files.writeString(file, content);

        IOException exception = assertThrows(IOException.class, () -> IniConfig.load(file));

        assertTrue(exception.getMessage().contains(expectedReason));
        assertTrue(exception.getMessage().contains("line"));
    }
}
