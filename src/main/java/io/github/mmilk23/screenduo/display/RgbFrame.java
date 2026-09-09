package io.github.mmilk23.screenduo.display;

import java.util.Arrays;

public final class RgbFrame {

    private static final int CHANNELS_PER_PIXEL = 3;

    private final DisplayGeometry geometry;
    private final byte[] pixels;

    public RgbFrame(DisplayGeometry geometry) {
        this.geometry = geometry;
        this.pixels = new byte[Math.multiplyExact(geometry.pixelCount(), CHANNELS_PER_PIXEL)];
    }

    public DisplayGeometry geometry() {
        return geometry;
    }

    public void setPixel(int x, int y, int red, int green, int blue) {
        requireCoordinates(x, y);
        int offset = (y * geometry.width() + x) * CHANNELS_PER_PIXEL;
        pixels[offset] = channel(red);
        pixels[offset + 1] = channel(green);
        pixels[offset + 2] = channel(blue);
    }

    public void fillRectangle(int x, int y, int width, int height, int red, int green, int blue) {
        if (width < 0 || height < 0 || x < 0 || y < 0
                || x + width > geometry.width() || y + height > geometry.height()) {
            throw new IllegalArgumentException("Rectangle exceeds frame bounds");
        }

        for (int currentY = y; currentY < y + height; currentY++) {
            for (int currentX = x; currentX < x + width; currentX++) {
                setPixel(currentX, currentY, red, green, blue);
            }
        }
    }

    public byte[] pixels() {
        return Arrays.copyOf(pixels, pixels.length);
    }

    public int redAt(int x, int y) {
        return channelAt(x, y, 0);
    }

    public int greenAt(int x, int y) {
        return channelAt(x, y, 1);
    }

    public int blueAt(int x, int y) {
        return channelAt(x, y, 2);
    }

    private int channelAt(int x, int y, int channel) {
        requireCoordinates(x, y);
        int offset = (y * geometry.width() + x) * CHANNELS_PER_PIXEL;
        return Byte.toUnsignedInt(pixels[offset + channel]);
    }

    private void requireCoordinates(int x, int y) {
        if (x < 0 || x >= geometry.width() || y < 0 || y >= geometry.height()) {
            throw new IndexOutOfBoundsException("Pixel outside frame: " + x + "," + y);
        }
    }

    private static byte channel(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("RGB channels must be between 0 and 255");
        }
        return (byte) value;
    }
}
