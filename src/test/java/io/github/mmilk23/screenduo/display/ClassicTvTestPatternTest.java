package io.github.mmilk23.screenduo.display;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ClassicTvTestPatternTest {

    @Test
    void rendersAtRequestedResolution() {
        DisplayGeometry geometry = new DisplayGeometry(320, 240);

        RgbFrame frame = ClassicTvTestPattern.render(geometry);

        assertEquals(geometry, frame.geometry());
        assertEquals(320 * 240 * 3, frame.pixels().length);
    }

    @Test
    void startsWithWhiteBorderAndContainsYellowBar() {
        RgbFrame frame = ClassicTvTestPattern.render(new DisplayGeometry(320, 240));

        assertEquals(255, frame.redAt(0, 0));
        assertEquals(255, frame.greenAt(0, 0));
        assertEquals(255, frame.blueAt(0, 0));

        assertEquals(255, frame.redAt(68, 20));
        assertEquals(255, frame.greenAt(68, 20));
        assertEquals(0, frame.blueAt(68, 20));
    }
}
