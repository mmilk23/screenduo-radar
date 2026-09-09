package io.github.mmilk23.screenduo.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CompactPixelTextTest {

    @Test
    void calculatesCompactWidthsIncludingSpacesAndScale() {
        assertEquals(0, CompactPixelText.width("", 1));
        assertEquals(3, CompactPixelText.width(" ", 1));
        assertEquals(5, CompactPixelText.width("A", 1));
        assertEquals(15, CompactPixelText.width("A A", 1));
        assertEquals(10, CompactPixelText.width("A", 2));
    }

    @Test
    void drawsVisibleGlyphPixelsAndLeavesSpaceBlank() {
        RgbFrame frame = new RgbFrame(new DisplayGeometry(30, 14));
        FrameCanvas canvas = new FrameCanvas(frame);
        RgbColor color = new RgbColor(20, 30, 40);

        CompactPixelText.draw(canvas, "A A", 0, 0, 1, color);

        assertTrue(countColoredPixels(frame) > 0);
        for (int y = 0; y < 7; y++) {
            for (int x = 6; x < 10; x++) {
                assertEquals(0, frame.redAt(x, y));
            }
        }
    }

    private static int countColoredPixels(RgbFrame frame) {
        int count = 0;
        for (int y = 0; y < frame.geometry().height(); y++) {
            for (int x = 0; x < frame.geometry().width(); x++) {
                if (frame.redAt(x, y) != 0 || frame.greenAt(x, y) != 0 || frame.blueAt(x, y) != 0) {
                    count++;
                }
            }
        }
        return count;
    }
}
