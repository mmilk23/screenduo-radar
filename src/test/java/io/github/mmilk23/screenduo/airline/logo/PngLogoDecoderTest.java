package io.github.mmilk23.screenduo.airline.logo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mmilk23.screenduo.display.RgbFrame;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;
import org.junit.jupiter.api.Test;

class PngLogoDecoderTest {

    private static final byte[] SIGNATURE = new byte[] {
            (byte) 137, 80, 78, 71, 13, 10, 26, 10
    };

    @Test
    void decodesRgbAndExercisesAllPngFilters() throws Exception {
        byte[] raw = new byte[] {
                0, 0, 0, 0,
                1, 0, 0, 0,
                2, 0, 0, 0,
                3, 0, 0, 0,
                4, 0, 0, 0
        };

        RgbFrame frame = PngLogoDecoder.decodeAndFit(png(1, 5, 8, 2, 0, raw, null, null));

        assertEquals(RawRgbLogoStore.LOGO_GEOMETRY, frame.geometry());
        assertPixel(frame, 14, 0, 0, 0, 0);
        assertPixel(frame, 0, 0, 255, 255, 255);
    }

    @Test
    void compositesRgbaTransparencyAgainstWhite() throws Exception {
        byte[] raw = new byte[] {0, (byte) 255, 0, 0, (byte) 128};

        RgbFrame frame = PngLogoDecoder.decodeAndFit(png(1, 1, 8, 6, 0, raw, null, null));

        assertPixel(frame, 4, 0, 255, 127, 127);
    }

    @Test
    void decodesPaletteAndTransparency() throws Exception {
        byte[] palette = new byte[] {(byte) 10, (byte) 20, (byte) 30, (byte) 200, (byte) 100, (byte) 50};
        byte[] transparency = new byte[] {(byte) 255, (byte) 128};
        byte[] raw = new byte[] {0, 1};

        RgbFrame frame = PngLogoDecoder.decodeAndFit(png(1, 1, 8, 3, 0, raw, palette, transparency));

        assertPixel(frame, 4, 0, 227, 177, 152);
    }

    @Test
    void rejectsInvalidSignatureAndMissingIend() {
        assertThrows(IOException.class, () -> PngLogoDecoder.decodeAndFit(new byte[] {1, 2, 3}));
        assertThrows(IOException.class, () -> PngLogoDecoder.decodeAndFit(SIGNATURE.clone()));
    }

    @Test
    void rejectsUnsupportedFormatDimensionsColorTypeAndFilter() throws Exception {
        IOException bitDepth = assertThrows(IOException.class,
                () -> PngLogoDecoder.decodeAndFit(png(1, 1, 16, 2, 0, new byte[] {0, 0, 0, 0}, null, null)));
        assertTrue(bitDepth.getMessage().contains("8-bit"));

        IOException interlace = assertThrows(IOException.class,
                () -> PngLogoDecoder.decodeAndFit(png(1, 1, 8, 2, 1, new byte[] {0, 0, 0, 0}, null, null)));
        assertTrue(interlace.getMessage().contains("non-interlaced"));

        IOException dimensions = assertThrows(IOException.class,
                () -> PngLogoDecoder.decodeAndFit(png(0, 1, 8, 2, 0, new byte[0], null, null)));
        assertTrue(dimensions.getMessage().contains("dimensions"));

        IOException colorType = assertThrows(IOException.class,
                () -> PngLogoDecoder.decodeAndFit(png(1, 1, 8, 0, 0, new byte[] {0, 0}, null, null)));
        assertTrue(colorType.getMessage().contains("color type"));

        IOException filter = assertThrows(IOException.class,
                () -> PngLogoDecoder.decodeAndFit(png(1, 1, 8, 2, 0, new byte[] {5, 0, 0, 0}, null, null)));
        assertTrue(filter.getMessage().contains("filter"));
    }

    @Test
    void rejectsPaletteIndexOutsidePaletteAndBadCompression() throws Exception {
        IOException palette = assertThrows(IOException.class,
                () -> PngLogoDecoder.decodeAndFit(png(1, 1, 8, 3, 0, new byte[] {0, 2},
                        new byte[] {1, 2, 3}, null)));
        assertTrue(palette.getMessage().contains("palette index"));

        byte[] malformed = pngWithCompressedData(1, 1, 8, 2, 0, new byte[] {1, 2, 3, 4, 5});
        IOException compression = assertThrows(IOException.class,
                () -> PngLogoDecoder.decodeAndFit(malformed));
        assertTrue(compression.getMessage().contains("compression")
                || compression.getMessage().contains("inflater"));
    }

    @Test
    void rejectsInvalidChunkLength() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(SIGNATURE);
        try (DataOutputStream data = new DataOutputStream(output)) {
            data.writeInt(Integer.MAX_VALUE);
            data.writeBytes("IHDR");
            data.writeInt(0);
        }

        assertTrue(assertThrows(IOException.class,
                () -> PngLogoDecoder.decodeAndFit(output.toByteArray()))
                .getMessage().contains("chunk length"));
    }

    private static byte[] png(
            int width, int height, int bitDepth, int colorType, int interlace,
            byte[] raw, byte[] palette, byte[] transparency) throws IOException {
        return pngWithData(width, height, bitDepth, colorType, interlace,
                compress(raw), palette, transparency);
    }

    private static byte[] pngWithCompressedData(
            int width, int height, int bitDepth, int colorType, int interlace,
            byte[] compressed) throws IOException {
        return pngWithData(width, height, bitDepth, colorType, interlace,
                compressed, null, null);
    }

    private static byte[] pngWithData(
            int width, int height, int bitDepth, int colorType, int interlace,
            byte[] compressed, byte[] palette, byte[] transparency) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(SIGNATURE);
        ByteArrayOutputStream header = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(header)) {
            data.writeInt(width);
            data.writeInt(height);
            data.writeByte(bitDepth);
            data.writeByte(colorType);
            data.writeByte(0);
            data.writeByte(0);
            data.writeByte(interlace);
        }
        writeChunk(output, "IHDR", header.toByteArray());
        if (palette != null) {
            writeChunk(output, "PLTE", palette);
        }
        if (transparency != null) {
            writeChunk(output, "tRNS", transparency);
        }
        writeChunk(output, "IDAT", compressed);
        writeChunk(output, "IEND", new byte[0]);
        return output.toByteArray();
    }

    private static byte[] compress(byte[] raw) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(output)) {
            deflater.write(raw);
        }
        return output.toByteArray();
    }

    private static void writeChunk(ByteArrayOutputStream output, String type, byte[] payload)
            throws IOException {
        DataOutputStream data = new DataOutputStream(output);
        data.writeInt(payload.length);
        data.write(type.getBytes(StandardCharsets.US_ASCII));
        data.write(payload);
        data.writeInt(0);
        data.flush();
    }

    private static void assertPixel(RgbFrame frame, int x, int y, int red, int green, int blue) {
        assertEquals(red, frame.redAt(x, y));
        assertEquals(green, frame.greenAt(x, y));
        assertEquals(blue, frame.blueAt(x, y));
    }
}
