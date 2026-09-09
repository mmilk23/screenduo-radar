package io.github.mmilk23.screenduo.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SevenSegmentTextTest {

    @Test
    void calculatesWidthsAndLargestFittingSize() {
        assertEquals(0, SevenSegmentText.width("", 32));
        assertTrue(SevenSegmentText.width("12:34", 32) > SevenSegmentText.width("12", 32));

        int size = SevenSegmentText.largestFittingSize("12:34", 100, 40, 12);
        assertTrue(size >= 12 && size <= 40);
        assertTrue(SevenSegmentText.width("12:34", size) <= 100 || size == 12);
        assertEquals(12, SevenSegmentText.largestFittingSize("12:34", 1, 40, 12));
    }

    @Test
    void drawsDigitsColonAndIgnoresUnsupportedCharacters() {
        RgbFrame frame = new RgbFrame(new DisplayGeometry(120, 50));
        RgbColor color = new RgbColor(200, 100, 50);

        SevenSegmentText.draw(frame, "8:X", -2, -2, 32, color);

        assertTrue(countColoredPixels(frame) > 0);
        assertEquals(0, frame.redAt(119, 49));
    }

    @Test
    void rendersEveryDigit() {
        RgbFrame frame = new RgbFrame(new DisplayGeometry(320, 60));

        SevenSegmentText.draw(frame, "0123456789", 0, 0, 32, new RgbColor(255, 255, 255));

        assertTrue(countColoredPixels(frame) > 500);
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
