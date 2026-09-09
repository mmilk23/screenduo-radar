package io.github.mmilk23.screenduo.clock.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.weather.WeatherConditions;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AirportClockScreenRendererTest {
    private static final WeatherConditions WEATHER = new WeatherConditions(
            Instant.parse("2026-09-07T12:00:00Z"), 24.0, 25.0, 60, 0.0, 0, true, 0.5, 0, 1013.0, 12.0, 90.0);

    @Test
    void rendersWeatherHeaderAndDigitalClock() {
        var frame = new AirportClockScreenRenderer().render(
                new DisplayGeometry(320, 240),
                new AirportClockScreenData(
                        "Rio de Janeiro", WEATHER, Instant.parse("2026-09-07T12:34:56Z"), ZoneOffset.UTC));

        assertEquals(320 * 240 * 3, frame.pixels().length);
        assertEquals(12, frame.redAt(0, 0));
        assertEquals(5, frame.redAt(0, 40));
        assertTrue(hasAmberDatePixels(frame));
    }

    @Test
    void letterboxesWhenTargetGeometryIsWider() {
        var frame = new AirportClockScreenRenderer().render(
                new DisplayGeometry(640, 360),
                new AirportClockScreenData("Rio", WEATHER, Instant.parse("2026-09-07T12:34:56Z"), ZoneOffset.UTC));

        assertEquals(640 * 360 * 3, frame.pixels().length);
        assertEquals(5, frame.redAt(0, 0));
        assertEquals(12, frame.greenAt(0, 0));
        assertEquals(28, frame.blueAt(0, 0));
    }
    private static boolean hasAmberDatePixels(io.github.mmilk23.screenduo.display.RgbFrame frame) {
        for (int y = 182; y < 203; y++) {
            for (int x = 20; x < 300; x++) {
                if (frame.redAt(x, y) > 180 && frame.greenAt(x, y) > 100 && frame.blueAt(x, y) < 90) {
                    return true;
                }
            }
        }
        return false;
    }
}

