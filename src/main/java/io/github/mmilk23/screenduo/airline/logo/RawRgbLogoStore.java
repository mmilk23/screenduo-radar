package io.github.mmilk23.screenduo.airline.logo;

import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.RgbFrame;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

final class RawRgbLogoStore {

    static final DisplayGeometry LOGO_GEOMETRY = new DisplayGeometry(32, 24);
    private static final int RGB_BYTES_PER_PIXEL = 3;

    private RawRgbLogoStore() {
    }

    static RgbFrame read(Path file) throws IOException {
        byte[] bytes = Files.readAllBytes(file);
        int expected = LOGO_GEOMETRY.pixelCount() * RGB_BYTES_PER_PIXEL;
        if (bytes.length != expected) {
            throw new IOException("Unexpected logo RGB size: " + file);
        }
        RgbFrame frame = new RgbFrame(LOGO_GEOMETRY);
        int offset = 0;
        for (int y = 0; y < LOGO_GEOMETRY.height(); y++) {
            for (int x = 0; x < LOGO_GEOMETRY.width(); x++) {
                frame.setPixel(x, y,
                        Byte.toUnsignedInt(bytes[offset]),
                        Byte.toUnsignedInt(bytes[offset + 1]),
                        Byte.toUnsignedInt(bytes[offset + 2]));
                offset += RGB_BYTES_PER_PIXEL;
            }
        }
        return frame;
    }

    static void write(Path file, RgbFrame frame) throws IOException {
        if (!LOGO_GEOMETRY.equals(frame.geometry())) {
            throw new IOException("Unexpected logo geometry: " + frame.geometry());
        }
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, file.getFileName().toString(), ".part");
        try {
            byte[] bytes = new byte[LOGO_GEOMETRY.pixelCount() * RGB_BYTES_PER_PIXEL];
            int offset = 0;
            for (int y = 0; y < LOGO_GEOMETRY.height(); y++) {
                for (int x = 0; x < LOGO_GEOMETRY.width(); x++) {
                    bytes[offset] = (byte) frame.redAt(x, y);
                    bytes[offset + 1] = (byte) frame.greenAt(x, y);
                    bytes[offset + 2] = (byte) frame.blueAt(x, y);
                    offset += RGB_BYTES_PER_PIXEL;
                }
            }
            Files.write(temporary, bytes);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
