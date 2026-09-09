package io.github.mmilk23.screenduo.display;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FrameScalerTest {

    @Test
    void scalesWithNearestNeighborAndLetterboxes() {
        RgbFrame source = new RgbFrame(new DisplayGeometry(2, 1));
        source.setPixel(0, 0, 255, 0, 0);
        source.setPixel(1, 0, 0, 255, 0);
        RgbColor background = new RgbColor(1, 2, 3);

        RgbFrame target = FrameScaler.fit(source, new DisplayGeometry(4, 4), background);

        assertEquals(new DisplayGeometry(4, 4), target.geometry());
        assertEquals(1, target.redAt(0, 0));
        assertEquals(255, target.redAt(0, 1));
        assertEquals(255, target.redAt(1, 2));
        assertEquals(255, target.greenAt(2, 1));
        assertEquals(255, target.greenAt(3, 2));
        assertEquals(3, target.blueAt(3, 3));
    }

    @Test
    void downsizesSourceWithoutChangingAspectRatio() {
        RgbFrame source = new RgbFrame(new DisplayGeometry(4, 2));
        source.setPixel(0, 0, 10, 0, 0);
        source.setPixel(2, 0, 20, 0, 0);
        source.setPixel(0, 1, 30, 0, 0);
        source.setPixel(2, 1, 40, 0, 0);

        RgbFrame target = FrameScaler.fit(source, new DisplayGeometry(2, 1), new RgbColor(0, 0, 0));

        assertEquals(10, target.redAt(0, 0));
        assertEquals(20, target.redAt(1, 0));
    }
}
