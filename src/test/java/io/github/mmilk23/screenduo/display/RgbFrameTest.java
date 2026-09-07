package io.github.mmilk23.screenduo.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RgbFrameTest {

    @Test
    void storesRgbPixels() {
        RgbFrame frame = new RgbFrame(new DisplayGeometry(2, 1));

        frame.setPixel(1, 0, 10, 20, 255);

        assertEquals(10, frame.redAt(1, 0));
        assertEquals(20, frame.greenAt(1, 0));
        assertEquals(255, frame.blueAt(1, 0));
    }

    @Test
    void rejectsPixelsOutsideFrame() {
        RgbFrame frame = new RgbFrame(new DisplayGeometry(2, 2));

        assertThrows(IndexOutOfBoundsException.class, () -> frame.setPixel(2, 0, 0, 0, 0));
    }
}
