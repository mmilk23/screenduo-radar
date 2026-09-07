package io.github.mmilk23.screenduo.weather.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.RgbFrame;
import io.github.mmilk23.screenduo.weather.WeatherConditions;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class WeatherScreenRendererTest {

    private static final WeatherConditions WEATHER = new WeatherConditions(
            Instant.EPOCH, 17.2, 17.0, 84, 0.1, 51, 100, 1021.0, 11.4, 195.0);

    @Test
    void rendersWeatherAtScreenDuoResolution() {
        RgbFrame frame =
                new WeatherScreenRenderer().render(new DisplayGeometry(320, 240), WEATHER);

        assertEquals(320 * 240 * 3, frame.pixels().length);
        assertTrue(nonBlackPixels(frame) > 20_000);
    }

    @Test
    void scalesDashboardForAnotherDisplay() {
        DisplayGeometry geometry = new DisplayGeometry(640, 360);

        RgbFrame frame = new WeatherScreenRenderer().render(geometry, WEATHER);

        assertEquals(geometry, frame.geometry());
        assertEquals(640 * 360 * 3, frame.pixels().length);
    }

    @Test
    void translatesWmoWeatherCodes() {
        assertEquals("CLEAR", WeatherScreenRenderer.description(0));
        assertEquals("DRIZZLE", WeatherScreenRenderer.description(51));
        assertEquals("RAIN", WeatherScreenRenderer.description(80));
        assertEquals("STORM", WeatherScreenRenderer.description(95));
        assertEquals("NO DATA", WeatherScreenRenderer.description(null));
    }

    private static int nonBlackPixels(RgbFrame frame) {
        byte[] pixels = frame.pixels();
        int count = 0;
        for (int index = 0; index < pixels.length; index += 3) {
            if (pixels[index] != 0 || pixels[index + 1] != 0 || pixels[index + 2] != 0) {
                count++;
            }
        }
        return count;
    }
}
