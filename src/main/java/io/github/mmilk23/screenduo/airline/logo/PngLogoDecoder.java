package io.github.mmilk23.screenduo.airline.logo;

import io.github.mmilk23.screenduo.display.RgbFrame;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

final class PngLogoDecoder {

    private static final byte[] SIGNATURE = new byte[] {
            (byte) 137, 80, 78, 71, 13, 10, 26, 10
    };

    private PngLogoDecoder() {
    }

    static RgbFrame decodeAndFit(byte[] png) throws IOException {
        DecodedImage image = decode(png);
        RgbFrame frame = new RgbFrame(RawRgbLogoStore.LOGO_GEOMETRY);
        double ratio = Math.min(
                (double) frame.geometry().width() / image.width,
                (double) frame.geometry().height() / image.height);
        int width = Math.max(1, (int) Math.round(image.width * ratio));
        int height = Math.max(1, (int) Math.round(image.height * ratio));
        int startX = (frame.geometry().width() - width) / 2;
        int startY = (frame.geometry().height() - height) / 2;
        for (int y = 0; y < frame.geometry().height(); y++) {
            for (int x = 0; x < frame.geometry().width(); x++) {
                frame.setPixel(x, y, 255, 255, 255);
            }
        }
        for (int y = 0; y < height; y++) {
            int sourceY = Math.min(image.height - 1, (int) Math.floor(y / ratio));
            for (int x = 0; x < width; x++) {
                int sourceX = Math.min(image.width - 1, (int) Math.floor(x / ratio));
                int offset = (sourceY * image.width + sourceX) * 4;
                int alpha = Byte.toUnsignedInt(image.rgba[offset + 3]);
                int red = composite(Byte.toUnsignedInt(image.rgba[offset]), alpha);
                int green = composite(Byte.toUnsignedInt(image.rgba[offset + 1]), alpha);
                int blue = composite(Byte.toUnsignedInt(image.rgba[offset + 2]), alpha);
                frame.setPixel(startX + x, startY + y, red, green, blue);
            }
        }
        return frame;
    }

    private static int composite(int channel, int alpha) {
        return (channel * alpha + 255 * (255 - alpha)) / 255;
    }

    private static DecodedImage decode(byte[] png) throws IOException {
        if (png.length < SIGNATURE.length || !Arrays.equals(Arrays.copyOf(png, SIGNATURE.length), SIGNATURE)) {
            throw new IOException("Invalid PNG signature.");
        }
        ByteBuffer buffer = ByteBuffer.wrap(png).order(ByteOrder.BIG_ENDIAN);
        buffer.position(SIGNATURE.length);
        int width = 0;
        int height = 0;
        int colorType = -1;
        byte[] palette = new byte[0];
        byte[] transparency = new byte[0];
        ByteArrayOutputStream compressed = new ByteArrayOutputStream();
        while (buffer.remaining() >= 12) {
            int length = buffer.getInt();
            if (length < 0 || length > buffer.remaining() - 8) {
                throw new IOException("Invalid PNG chunk length.");
            }
            String type = new String(png, buffer.position(), 4, java.nio.charset.StandardCharsets.US_ASCII);
            buffer.position(buffer.position() + 4);
            byte[] data = new byte[length];
            buffer.get(data);
            buffer.getInt();
            switch (type) {
                case "IHDR" -> {
                    ByteBuffer header = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
                    width = header.getInt();
                    height = header.getInt();
                    int bitDepth = Byte.toUnsignedInt(data[8]);
                    colorType = Byte.toUnsignedInt(data[9]);
                    int interlace = Byte.toUnsignedInt(data[12]);
                    if (bitDepth != 8 || interlace != 0) {
                        throw new IOException("Only 8-bit non-interlaced PNG logos are supported.");
                    }
                }
                case "PLTE" -> palette = data;
                case "tRNS" -> transparency = data;
                case "IDAT" -> compressed.write(data, 0, data.length);
                case "IEND" -> {
                    return decodePixels(width, height, colorType, palette, transparency, compressed.toByteArray());
                }
                default -> { }
            }
        }
        throw new IOException("PNG is missing IEND.");
    }

    private static DecodedImage decodePixels(
            int width, int height, int colorType, byte[] palette, byte[] transparency, byte[] compressed)
            throws IOException {
        if (width <= 0 || height <= 0) {
            throw new IOException("Invalid PNG dimensions.");
        }
        int channels = switch (colorType) {
            case 2 -> 3;
            case 3 -> 1;
            case 6 -> 4;
            default -> throw new IOException("Unsupported PNG color type: " + colorType);
        };
        byte[] inflated = inflate(compressed, (width * channels + 1) * height);
        byte[] rgba = new byte[width * height * 4];
        byte[] previous = new byte[width * channels];
        byte[] current = new byte[width * channels];
        int input = 0;
        int output = 0;
        for (int y = 0; y < height; y++) {
            int filter = Byte.toUnsignedInt(inflated[input++]);
            System.arraycopy(inflated, input, current, 0, current.length);
            input += current.length;
            unfilter(current, previous, channels, filter);
            for (int x = 0; x < width; x++) {
                int pixel = x * channels;
                if (colorType == 2) {
                    rgba[output++] = current[pixel];
                    rgba[output++] = current[pixel + 1];
                    rgba[output++] = current[pixel + 2];
                    rgba[output++] = (byte) 255;
                } else if (colorType == 6) {
                    rgba[output++] = current[pixel];
                    rgba[output++] = current[pixel + 1];
                    rgba[output++] = current[pixel + 2];
                    rgba[output++] = current[pixel + 3];
                } else {
                    int index = Byte.toUnsignedInt(current[pixel]);
                    int paletteOffset = index * 3;
                    if (paletteOffset + 2 >= palette.length) {
                        throw new IOException("PNG palette index outside PLTE.");
                    }
                    rgba[output++] = palette[paletteOffset];
                    rgba[output++] = palette[paletteOffset + 1];
                    rgba[output++] = palette[paletteOffset + 2];
                    rgba[output++] = index < transparency.length ? transparency[index] : (byte) 255;
                }
            }
            byte[] swap = previous;
            previous = current;
            current = swap;
        }
        return new DecodedImage(width, height, rgba);
    }

    private static byte[] inflate(byte[] compressed, int expectedMinimum) throws IOException {
        Inflater inflater = new Inflater();
        inflater.setInput(compressed);
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.max(expectedMinimum, 1024));
        byte[] buffer = new byte[8192];
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                if (count == 0) {
                    if (inflater.needsInput()) {
                        break;
                    }
                    throw new IOException("PNG inflater made no progress.");
                }
                output.write(buffer, 0, count);
            }
        } catch (DataFormatException exception) {
            throw new IOException("Invalid PNG compression stream.", exception);
        } finally {
            inflater.end();
        }
        return output.toByteArray();
    }

    private static void unfilter(byte[] current, byte[] previous, int bytesPerPixel, int filter) throws IOException {
        for (int index = 0; index < current.length; index++) {
            int left = index >= bytesPerPixel ? Byte.toUnsignedInt(current[index - bytesPerPixel]) : 0;
            int up = Byte.toUnsignedInt(previous[index]);
            int upLeft = index >= bytesPerPixel ? Byte.toUnsignedInt(previous[index - bytesPerPixel]) : 0;
            int value = Byte.toUnsignedInt(current[index]);
            int result = switch (filter) {
                case 0 -> value;
                case 1 -> value + left;
                case 2 -> value + up;
                case 3 -> value + ((left + up) / 2);
                case 4 -> value + paeth(left, up, upLeft);
                default -> throw new IOException("Unsupported PNG filter: " + filter);
            };
            current[index] = (byte) (result & 0xff);
        }
    }

    private static int paeth(int left, int up, int upLeft) {
        int estimate = left + up - upLeft;
        int leftDistance = Math.abs(estimate - left);
        int upDistance = Math.abs(estimate - up);
        int upLeftDistance = Math.abs(estimate - upLeft);
        if (leftDistance <= upDistance && leftDistance <= upLeftDistance) {
            return left;
        }
        return upDistance <= upLeftDistance ? up : upLeft;
    }

    private record DecodedImage(int width, int height, byte[] rgba) {
    }
}
