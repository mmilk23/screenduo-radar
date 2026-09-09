package io.github.mmilk23.screenduo.clock.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.RgbFrame;
import io.github.mmilk23.screenduo.weather.WeatherConditions;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class AirportClockScreenRendererTest {
    private static final DisplayGeometry GEOMETRY = new DisplayGeometry(320, 240);
    private static final Instant NOW = Instant.parse("2026-09-07T12:34:56Z");
    private static final WeatherConditions WEATHER = weather(0, true, 0.5, 24.0);

    @Test
    void rendersWeatherHeaderAndDigitalClock() {
        var frame = render("Rio de Janeiro", WEATHER);

        assertEquals(320 * 240 * 3, frame.pixels().length);
        assertEquals(12, frame.redAt(0, 0));
        assertEquals(5, frame.redAt(0, 40));
        assertTrue(hasAmberDatePixels(frame));
        assertTrue(hasColor(frame, 255, 205, 64));
    }

    @Test
    void letterboxesWhenTargetGeometryIsWider() {
        var frame = new AirportClockScreenRenderer().render(
                new DisplayGeometry(640, 360),
                new AirportClockScreenData("Rio", WEATHER, NOW, ZoneOffset.UTC));

        assertEquals(640 * 360 * 3, frame.pixels().length);
        assertEquals(5, frame.redAt(0, 0));
        assertEquals(12, frame.greenAt(0, 0));
        assertEquals(28, frame.blueAt(0, 0));
    }

    @Test
    void rendersAllWeatherConditionFamilies() {
        Integer[] codes = {null, 0, 1, 45, 48, 51, 57, 61, 67, 80, 82, 71, 77, 85, 86, 95, 99, 4};

        for (Integer code : codes) {
            RgbFrame frame = render("Rio", weather(code, true, 0.5, 24.0));
            assertEquals(320 * 240 * 3, frame.pixels().length, "WMO code " + code);
        }

        assertTrue(hasColor(render("Rio", weather(61, true, 0.5, 24.0)), 70, 160, 255));
        assertTrue(hasColor(render("Rio", weather(71, true, 0.5, 24.0)), 238, 246, 255));
        assertTrue(hasColor(render("Rio", weather(95, true, 0.5, 24.0)), 255, 205, 64));
    }

    @Test
    void rendersMoonAcrossAllPhasesAndHandlesMissingOrOutOfRangePhase() {
        Double[] phases = {null, -1.0, 0.0, 0.125, 0.25, 0.375, 0.5, 0.625, 0.75, 0.875, 1.0, 2.0};

        for (Double phase : phases) {
            RgbFrame frame = render("Rio", weather(0, false, phase, 24.0));
            assertEquals(320 * 240 * 3, frame.pixels().length, "Moon phase " + phase);
        }
    }

    @Test
    void treatsMissingDayFlagAsDayForClearWeather() {
        RgbFrame frame = render("Rio", weather(0, null, 0.5, 24.0));

        assertTrue(hasColor(frame, 255, 205, 64));
    }

    @Test
    void rendersMissingTemperature() {
        RgbFrame frame = render("Rio", weather(0, true, 0.5, null));

        assertEquals(320 * 240 * 3, frame.pixels().length);
    }

    @Test
    void rendersShortMediumAndTruncatedCityTitles() {
        String[] cities = {
            "Rio",
            "Rio de Janeiro",
            "Sao Jose dos Campos",
            "Aeroporto Internacional Metropolitano Com Nome Deliberadamente Muito Longo"
        };

        for (String city : cities) {
            RgbFrame frame = render(city, WEATHER);
            assertEquals(320 * 240 * 3, frame.pixels().length, city);
        }
    }

    @Test
    void normalizesAccentedCityName() {
        RgbFrame accented = render("São José", WEATHER);
        RgbFrame normalized = render("SAO JOSE", WEATHER);

        assertEquals(java.util.Arrays.toString(normalized.pixels()), java.util.Arrays.toString(accented.pixels()));
    }

    private static RgbFrame render(String city, WeatherConditions weather) {
        return new AirportClockScreenRenderer().render(
                GEOMETRY,
                new AirportClockScreenData(city, weather, NOW, ZoneOffset.UTC));
    }

    private static WeatherConditions weather(Integer code, Boolean isDay, Double moonPhase, Double temperature) {
        return new WeatherConditions(
                Instant.parse("2026-09-07T12:00:00Z"),
                temperature,
                25.0,
                60,
                0.0,
                code,
                isDay,
                moonPhase,
                0,
                1013.0,
                12.0,
                90.0);
    }

    private static boolean hasAmberDatePixels(RgbFrame frame) {
        for (int y = 182; y < 203; y++) {
            for (int x = 20; x < 300; x++) {
                if (frame.redAt(x, y) > 180 && frame.greenAt(x, y) > 100 && frame.blueAt(x, y) < 90) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasColor(RgbFrame frame, int red, int green, int blue) {
        for (int y = 0; y < frame.geometry().height(); y++) {
            for (int x = 0; x < frame.geometry().width(); x++) {
                if (frame.redAt(x, y) == red && frame.greenAt(x, y) == green && frame.blueAt(x, y) == blue) {
                    return true;
                }
            }
        }
        return false;
    }
}
