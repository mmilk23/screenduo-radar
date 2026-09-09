package io.github.mmilk23.screenduo.display;

import java.util.Locale;

/** Draws a compact seven-segment clock without AWT dependencies. */
public final class SevenSegmentText {

    private static final int[][] DIGIT_SEGMENTS = {
            {0, 1, 2, 3, 4, 5},
            {1, 2},
            {0, 1, 6, 4, 3},
            {0, 1, 6, 2, 3},
            {5, 6, 1, 2},
            {0, 5, 6, 2, 3},
            {0, 5, 6, 4, 2, 3},
            {0, 1, 2},
            {0, 1, 2, 3, 4, 5, 6},
            {0, 1, 2, 3, 5, 6}
    };

    private SevenSegmentText() {
    }

    public static int largestFittingSize(String text, int maximumWidth, int maximumSize, int minimumSize) {
        for (int size = maximumSize; size >= minimumSize; size--) {
            if (width(text, size) <= maximumWidth) {
                return size;
            }
        }
        return minimumSize;
    }

    public static int width(String text, int size) {
        int total = 0;
        for (int index = 0; index < text.length(); index++) {
            if (index > 0) {
                total += spacing(size);
            }
            total += characterWidth(text.charAt(index), size);
        }
        return total;
    }

    public static void draw(RgbFrame frame, String text, int x, int topY, int size, RgbColor color) {
        int cursor = x;
        for (int index = 0; index < text.length(); index++) {
            if (index > 0) {
                cursor += spacing(size);
            }
            char value = text.charAt(index);
            drawCharacter(frame, value, cursor, topY, size, color);
            cursor += characterWidth(value, size);
        }
    }

    private static void drawCharacter(RgbFrame frame, char value, int x, int y, int size, RgbColor color) {
        if (value == ':') {
            drawColon(frame, x, y, size, color);
            return;
        }
        if (value < '0' || value > '9') {
            return;
        }
        for (int segment : DIGIT_SEGMENTS[value - '0']) {
            drawSegment(frame, segment, x, y, size, color);
        }
    }

    private static void drawColon(RgbFrame frame, int x, int y, int size, RgbColor color) {
        int dot = Math.max(3, size / 10);
        int left = x + Math.max(1, dot / 2);
        fill(frame, left, y + size / 3 - dot / 2, dot, dot, color);
        fill(frame, left, y + (size * 2) / 3 - dot / 2, dot, dot, color);
    }

    private static void drawSegment(RgbFrame frame, int segment, int x, int y, int size, RgbColor color) {
        int thickness = Math.max(4, size / 8);
        int width = digitWidth(size);
        int half = size / 2;
        switch (segment) {
            case 0 -> fill(frame, x + thickness, y, width - thickness * 2, thickness, color);
            case 1 -> fill(frame, x + width - thickness, y + thickness, thickness, half - thickness, color);
            case 2 -> fill(frame, x + width - thickness, y + half, thickness, half - thickness, color);
            case 3 -> fill(frame, x + thickness, y + size - thickness, width - thickness * 2, thickness, color);
            case 4 -> fill(frame, x, y + half, thickness, half - thickness, color);
            case 5 -> fill(frame, x, y + thickness, thickness, half - thickness, color);
            case 6 -> fill(frame, x + thickness, y + half - thickness / 2, width - thickness * 2, thickness, color);
            default -> throw new IllegalArgumentException(String.format(Locale.ROOT, "Invalid segment %d", segment));
        }
    }


    private static void fill(RgbFrame frame, int x, int y, int width, int height, RgbColor color) {
        int left = Math.max(0, x);
        int top = Math.max(0, y);
        int right = Math.min(frame.geometry().width(), x + width);
        int bottom = Math.min(frame.geometry().height(), y + height);
        for (int row = top; row < bottom; row++) {
            for (int column = left; column < right; column++) {
                frame.setPixel(column, row, color.red(), color.green(), color.blue());
            }
        }
    }

    private static int characterWidth(char value, int size) {
        return value == ':' ? Math.max(5, size / 6) : digitWidth(size);
    }

    private static int digitWidth(int size) {
        return Math.max(16, size / 2 + size / 8);
    }

    private static int spacing(int size) {
        return Math.max(2, size / 16);
    }
}
