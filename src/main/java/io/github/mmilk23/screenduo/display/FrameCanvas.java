package io.github.mmilk23.screenduo.display;

public final class FrameCanvas {

    private final RgbFrame frame;

    public FrameCanvas(RgbFrame frame) {
        this.frame = frame;
    }

    public void clear(RgbColor color) {
        frame.fillRectangle(
                0, 0, frame.geometry().width(), frame.geometry().height(),
                color.red(), color.green(), color.blue());
    }

    public void fillRectangle(int x, int y, int width, int height, RgbColor color) {
        int startX = Math.max(0, x);
        int startY = Math.max(0, y);
        int endX = Math.min(frame.geometry().width(), x + width);
        int endY = Math.min(frame.geometry().height(), y + height);
        if (startX >= endX || startY >= endY) {
            return;
        }
        frame.fillRectangle(
                startX, startY, endX - startX, endY - startY,
                color.red(), color.green(), color.blue());
    }

    public void drawLine(int fromX, int fromY, int toX, int toY, RgbColor color) {
        int deltaX = Math.abs(toX - fromX);
        int stepX = fromX < toX ? 1 : -1;
        int deltaY = -Math.abs(toY - fromY);
        int stepY = fromY < toY ? 1 : -1;
        int error = deltaX + deltaY;
        int x = fromX;
        int y = fromY;

        while (true) {
            setPixelIfVisible(x, y, color);
            if (x == toX && y == toY) {
                return;
            }
            int doubledError = 2 * error;
            if (doubledError >= deltaY) {
                error += deltaY;
                x += stepX;
            }
            if (doubledError <= deltaX) {
                error += deltaX;
                y += stepY;
            }
        }
    }

    public void fillCircle(int centerX, int centerY, int radius, RgbColor color) {
        int squaredRadius = radius * radius;
        for (int y = centerY - radius; y <= centerY + radius; y++) {
            for (int x = centerX - radius; x <= centerX + radius; x++) {
                int deltaX = x - centerX;
                int deltaY = y - centerY;
                if (deltaX * deltaX + deltaY * deltaY <= squaredRadius) {
                    setPixelIfVisible(x, y, color);
                }
            }
        }
    }

    public void drawText(String text, int x, int y, int scale, RgbColor color) {
        if (scale <= 0) {
            throw new IllegalArgumentException("Text scale must be positive.");
        }
        int cursorX = x;
        for (char character : text.toCharArray()) {
            drawCharacter(character, cursorX, y, scale, color);
            cursorX += 6 * scale;
        }
    }

    public int textWidth(String text, int scale) {
        return text.isEmpty() ? 0 : text.length() * 6 * scale - scale;
    }

    private void drawCharacter(char character, int x, int y, int scale, RgbColor color) {
        int[] glyph = PixelFont.glyph(character);
        for (int row = 0; row < glyph.length; row++) {
            for (int column = 0; column < 5; column++) {
                if ((glyph[row] & (1 << (4 - column))) != 0) {
                    fillRectangle(
                            x + column * scale,
                            y + row * scale,
                            scale,
                            scale,
                            color);
                }
            }
        }
    }

    private void setPixelIfVisible(int x, int y, RgbColor color) {
        if (x >= 0 && x < frame.geometry().width()
                && y >= 0 && y < frame.geometry().height()) {
            frame.setPixel(x, y, color.red(), color.green(), color.blue());
        }
    }
}
