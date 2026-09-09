package io.github.mmilk23.screenduo.airline.logo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.RgbFrame;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RawRgbLogoStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void writesAndReadsRgbFrameWithoutLosingChannelValues() throws Exception {
        RgbFrame frame = new RgbFrame(RawRgbLogoStore.LOGO_GEOMETRY);
        frame.setPixel(0, 0, 0, 127, 255);
        frame.setPixel(31, 23, 254, 128, 1);
        Path file = tempDir.resolve("nested").resolve("logo.rgb");

        RawRgbLogoStore.write(file, frame);
        RgbFrame restored = RawRgbLogoStore.read(file);

        assertTrue(Files.isRegularFile(file));
        assertEquals(RawRgbLogoStore.LOGO_GEOMETRY.pixelCount() * 3L, Files.size(file));
        assertEquals(0, restored.redAt(0, 0));
        assertEquals(127, restored.greenAt(0, 0));
        assertEquals(255, restored.blueAt(0, 0));
        assertEquals(254, restored.redAt(31, 23));
        assertEquals(128, restored.greenAt(31, 23));
        assertEquals(1, restored.blueAt(31, 23));
    }

    @Test
    void rejectsUnexpectedFileSize() throws Exception {
        Path file = tempDir.resolve("invalid.rgb");
        Files.write(file, new byte[] {1, 2, 3});

        assertThrows(IOException.class, () -> RawRgbLogoStore.read(file));
    }

    @Test
    void rejectsUnexpectedFrameGeometry() {
        RgbFrame frame = new RgbFrame(new DisplayGeometry(16, 16));

        assertThrows(IOException.class, () -> RawRgbLogoStore.write(tempDir.resolve("logo.rgb"), frame));
    }

    @Test
    void replacesExistingLogoFile() throws Exception {
        Path file = tempDir.resolve("logo.rgb");
        Files.writeString(file, "old");
        RgbFrame frame = new RgbFrame(RawRgbLogoStore.LOGO_GEOMETRY);
        frame.setPixel(5, 5, 12, 34, 56);

        RawRgbLogoStore.write(file, frame);

        RgbFrame restored = RawRgbLogoStore.read(file);
        assertEquals(12, restored.redAt(5, 5));
        assertEquals(34, restored.greenAt(5, 5));
        assertEquals(56, restored.blueAt(5, 5));
    }
}
