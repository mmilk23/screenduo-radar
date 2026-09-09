package io.github.mmilk23.screenduo.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FrameCanvasTest {

    private static final RgbColor RED = new RgbColor(255, 0, 0);

    @Test
    void clearsFrameAndClipsRectangles() {
        RgbFrame frame = new RgbFrame(new DisplayGeometry(4, 3));
        FrameCanvas canvas = new FrameCanvas(frame);

        canvas.clear(new RgbColor(1, 2, 3));
        canvas.fillRectangle(-1, -1, 3, 3, RED);
        canvas.fillRectangle(20, 20, 2, 2, RED);

        assertPixel(frame, 0, 0, RED);
        assertPixel(frame, 1, 1, RED);
        assertEquals(1, frame.redAt(2, 2));
        assertEquals(2, frame.greenAt(2, 2));
        assertEquals(3, frame.blueAt(2, 2));
    }

    @Test
    void drawsLinesAndClipsInvisiblePixels() {
        RgbFrame frame = new RgbFrame(new DisplayGeometry(5, 5));
        FrameCanvas canvas = new FrameCanvas(frame);

        canvas.drawLine(-2, -2, 4, 4, RED);
        canvas.drawLine(4, 0, 0, 4, RED);

        assertPixel(frame, 0, 0, RED);
        assertPixel(frame, 2, 2, RED);
        assertPixel(frame, 4, 4, RED);
        assertPixel(frame, 4, 0, RED);
        assertPixel(frame, 0, 4, RED);
    }

    @Test
    void fillsCircleWithinFrameBounds() {
        RgbFrame frame = new RgbFrame(new DisplayGeometry(5, 5));
        FrameCanvas canvas = new FrameCanvas(frame);

        canvas.fillCircle(0, 0, 2, RED);

        assertPixel(frame, 0, 0, RED);
        assertPixel(frame, 2, 0, RED);
        assertPixel(frame, 0, 2, RED);
        assertEquals(0, frame.redAt(2, 2));
    }

    @Test
    void measuresAndDrawsTextAndRejectsInvalidScale() {
        RgbFrame frame = new RgbFrame(new DisplayGeometry(30, 14));
        FrameCanvas canvas = new FrameCanvas(frame);

        assertEquals(0, canvas.textWidth("", 2));
        assertEquals(10, canvas.textWidth("A", 2));
        assertEquals(22, canvas.textWidth("AB", 2));
        assertThrows(IllegalArgumentException.class, () -> canvas.drawText("A", 0, 0, 0, RED));

        canvas.drawText("A", 0, 0, 2, RED);
        assertPixel(frame, 2, 0, RED);
    }

    @Test
    void drawsScaledSourceFrameAndRejectsInvalidScale() {
        RgbFrame source = new RgbFrame(new DisplayGeometry(2, 1));
        source.setPixel(0, 0, 10, 20, 30);
        source.setPixel(1, 0, 40, 50, 60);
        RgbFrame target = new RgbFrame(new DisplayGeometry(4, 2));
        FrameCanvas canvas = new FrameCanvas(target);

        assertThrows(IllegalArgumentException.class, () -> canvas.drawFrame(source, 0, 0, 0));
        canvas.drawFrame(source, 0, 0, 2);

        assertEquals(10, target.redAt(0, 0));
        assertEquals(10, target.redAt(1, 1));
        assertEquals(40, target.redAt(2, 0));
        assertEquals(60, target.blueAt(3, 1));
    }

    private static void assertPixel(RgbFrame frame, int x, int y, RgbColor color) {
        assertEquals(color.red(), frame.redAt(x, y));
        assertEquals(color.green(), frame.greenAt(x, y));
        assertEquals(color.blue(), frame.blueAt(x, y));
    }
}
