package io.github.mmilk23.screenduo.display;

public final class ClassicTvTestPattern {

    private static final int[][] COLOR_BARS = {
        {255, 255, 255},
        {255, 255, 0},
        {0, 255, 255},
        {0, 255, 0},
        {255, 0, 255},
        {255, 0, 0},
        {0, 0, 255}
    };

    private ClassicTvTestPattern() {
    }

    public static RgbFrame render(DisplayGeometry geometry) {
        RgbFrame frame = new RgbFrame(geometry);
        int upperHeight = geometry.height() * 2 / 3;
        drawColorBars(frame, upperHeight);
        drawLowerCalibrationBars(frame, upperHeight);
        drawGrid(frame);
        drawCircle(frame);
        drawCrosshair(frame);
        drawBorder(frame);
        return frame;
    }

    private static void drawColorBars(RgbFrame frame, int height) {
        int width = frame.geometry().width();
        for (int index = 0; index < COLOR_BARS.length; index++) {
            int startX = index * width / COLOR_BARS.length;
            int endX = (index + 1) * width / COLOR_BARS.length;
            int[] color = COLOR_BARS[index];
            frame.fillRectangle(startX, 0, endX - startX, height, color[0], color[1], color[2]);
        }
    }

    private static void drawLowerCalibrationBars(RgbFrame frame, int startY) {
        int width = frame.geometry().width();
        int height = frame.geometry().height() - startY;
        int[][] colors = {
            {0, 0, 0},
            {48, 48, 48},
            {96, 96, 96},
            {160, 160, 160},
            {208, 208, 208},
            {255, 255, 255},
            {0, 0, 0}
        };

        for (int index = 0; index < colors.length; index++) {
            int startX = index * width / colors.length;
            int endX = (index + 1) * width / colors.length;
            int[] color = colors[index];
            frame.fillRectangle(startX, startY, endX - startX, height, color[0], color[1], color[2]);
        }
    }

    private static void drawGrid(RgbFrame frame) {
        int spacing = Math.max(16, Math.min(frame.geometry().width(), frame.geometry().height()) / 6);
        for (int x = spacing; x < frame.geometry().width(); x += spacing) {
            drawVerticalLine(frame, x, 96, 96, 96);
        }
        for (int y = spacing; y < frame.geometry().height(); y += spacing) {
            drawHorizontalLine(frame, y, 96, 96, 96);
        }
    }

    private static void drawCircle(RgbFrame frame) {
        int centerX = frame.geometry().width() / 2;
        int centerY = frame.geometry().height() / 2;
        int radius = Math.min(frame.geometry().width(), frame.geometry().height()) / 4;
        int tolerance = Math.max(2, radius / 24);
        int inner = (radius - tolerance) * (radius - tolerance);
        int outer = (radius + tolerance) * (radius + tolerance);

        for (int y = Math.max(0, centerY - radius - tolerance);
                y < Math.min(frame.geometry().height(), centerY + radius + tolerance); y++) {
            for (int x = Math.max(0, centerX - radius - tolerance);
                    x < Math.min(frame.geometry().width(), centerX + radius + tolerance); x++) {
                int deltaX = x - centerX;
                int deltaY = y - centerY;
                int distance = deltaX * deltaX + deltaY * deltaY;
                if (distance >= inner && distance <= outer) {
                    frame.setPixel(x, y, 255, 255, 255);
                }
            }
        }
    }

    private static void drawCrosshair(RgbFrame frame) {
        int centerX = frame.geometry().width() / 2;
        int centerY = frame.geometry().height() / 2;
        int arm = Math.min(frame.geometry().width(), frame.geometry().height()) / 10;

        for (int x = centerX - arm; x <= centerX + arm; x++) {
            frame.setPixel(x, centerY, 255, 255, 255);
        }
        for (int y = centerY - arm; y <= centerY + arm; y++) {
            frame.setPixel(centerX, y, 255, 255, 255);
        }
    }

    private static void drawBorder(RgbFrame frame) {
        drawHorizontalLine(frame, 0, 255, 255, 255);
        drawHorizontalLine(frame, frame.geometry().height() - 1, 255, 255, 255);
        drawVerticalLine(frame, 0, 255, 255, 255);
        drawVerticalLine(frame, frame.geometry().width() - 1, 255, 255, 255);
    }

    private static void drawHorizontalLine(RgbFrame frame, int y, int red, int green, int blue) {
        for (int x = 0; x < frame.geometry().width(); x++) {
            frame.setPixel(x, y, red, green, blue);
        }
    }

    private static void drawVerticalLine(RgbFrame frame, int x, int red, int green, int blue) {
        for (int y = 0; y < frame.geometry().height(); y++) {
            frame.setPixel(x, y, red, green, blue);
        }
    }
}
