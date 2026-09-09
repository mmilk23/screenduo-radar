package io.github.mmilk23.screenduo.display;

public final class CompactPixelText {

    private static final int GLYPH_HEIGHT = 7;

    private CompactPixelText() {
    }

    public static int width(String text, int scale) {
        if (text.isEmpty()) {
            return 0;
        }
        int width = 0;
        for (char character : text.toCharArray()) {
            width += glyphWidth(character) * scale + scale;
        }
        return width - scale;
    }

    public static void draw(FrameCanvas canvas, String text, int x, int y, int scale, RgbColor color) {
        int cursorX = x;
        for (char character : text.toCharArray()) {
            int firstColumn = firstColumn(character);
            int glyphWidth = glyphWidth(character);
            int[] glyph = PixelFont.glyph(character);
            for (int row = 0; row < GLYPH_HEIGHT; row++) {
                for (int column = 0; column < glyphWidth; column++) {
                    int sourceColumn = firstColumn + column;
                    if ((glyph[row] & (1 << (4 - sourceColumn))) != 0) {
                        canvas.fillRectangle(
                                cursorX + column * scale,
                                y + row * scale,
                                scale,
                                scale,
                                color);
                    }
                }
            }
            cursorX += glyphWidth * scale + scale;
        }
    }

    private static int glyphWidth(char character) {
        if (character == ' ') {
            return 3;
        }
        return lastColumn(character) - firstColumn(character) + 1;
    }

    private static int firstColumn(char character) {
        if (character == ' ') {
            return 0;
        }
        int[] glyph = PixelFont.glyph(character);
        for (int column = 0; column < 5; column++) {
            if (hasPixels(glyph, column)) {
                return column;
            }
        }
        return 0;
    }

    private static int lastColumn(char character) {
        if (character == ' ') {
            return 2;
        }
        int[] glyph = PixelFont.glyph(character);
        for (int column = 4; column >= 0; column--) {
            if (hasPixels(glyph, column)) {
                return column;
            }
        }
        return 4;
    }

    private static boolean hasPixels(int[] glyph, int column) {
        for (int row : glyph) {
            if ((row & (1 << (4 - column))) != 0) {
                return true;
            }
        }
        return false;
    }
}

