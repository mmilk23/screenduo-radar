package io.github.mmilk23.screenduo.airport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mmilk23.screenduo.display.Display;
import io.github.mmilk23.screenduo.display.DisplayButton;
import io.github.mmilk23.screenduo.display.DisplayButtonEvent;
import io.github.mmilk23.screenduo.display.DisplayControls;
import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.RgbFrame;
import io.github.mmilk23.screenduo.location.GeoPoint;
import io.github.mmilk23.screenduo.weather.WeatherConditions;
import io.github.mmilk23.screenduo.weather.WeatherProvider;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AirportBrowserControllerTest {

    @Test
    void navigatesLoadsWeatherReturnsToListAndExits() throws Exception {
        RecordingDisplay display = new RecordingDisplay();
        QueueControls controls = new QueueControls(
                DisplayButton.DOWN,
                DisplayButton.CONFIRM,
                DisplayButton.BACK,
                DisplayButton.UNKNOWN,
                DisplayButton.BACK);
        RecordingWeatherProvider weather = new RecordingWeatherProvider(sampleWeather());
        AirportBrowserController controller = new AirportBrowserController(
                display, controls, airports(), weather);

        controller.run();

        assertEquals(new GeoPoint(-23.6261, -46.6566), weather.requestedLocation);
        assertTrue(display.frames.size() >= 5);
    }

    @Test
    void wrapsSelectionUpFromFirstAirport() throws Exception {
        RecordingDisplay display = new RecordingDisplay();
        QueueControls controls = new QueueControls(
                DisplayButton.UP, DisplayButton.CONFIRM, DisplayButton.BACK,
                DisplayButton.UNKNOWN, DisplayButton.BACK);
        RecordingWeatherProvider weather = new RecordingWeatherProvider(sampleWeather());
        AirportBrowserController controller = new AirportBrowserController(
                display, controls, airports(), weather);

        controller.run();

        assertEquals(new GeoPoint(-23.6261, -46.6566), weather.requestedLocation);
    }

    @Test
    void ignoresUnknownAndDebouncesRepeatedButton() throws Exception {
        RecordingDisplay display = new RecordingDisplay();
        QueueControls controls = new QueueControls(
                DisplayButton.DOWN,
                DisplayButton.DOWN,
                DisplayButton.UNKNOWN,
                DisplayButton.DOWN,
                DisplayButton.CONFIRM,
                DisplayButton.BACK,
                DisplayButton.UNKNOWN,
                DisplayButton.BACK);
        RecordingWeatherProvider weather = new RecordingWeatherProvider(sampleWeather());
        AirportBrowserController controller = new AirportBrowserController(
                display, controls, airports(), weather);

        controller.run();

        assertEquals(new GeoPoint(-22.8090, -43.2506), weather.requestedLocation);
    }

    @Test
    void showsWeatherErrorAndAllowsBackNavigation() throws Exception {
        RecordingDisplay display = new RecordingDisplay();
        QueueControls controls = new QueueControls(
                DisplayButton.CONFIRM, DisplayButton.BACK, DisplayButton.UNKNOWN, DisplayButton.BACK);
        WeatherProvider weather = location -> { throw new IOException("offline"); };
        AirportBrowserController controller = new AirportBrowserController(
                display, controls, airports(), weather);

        controller.run();

        assertTrue(display.frames.size() >= 4);
    }

    @Test
    void handlesEmptyAirportListWithoutCallingWeather() throws Exception {
        RecordingDisplay display = new RecordingDisplay();
        QueueControls controls = new QueueControls(DisplayButton.UP, DisplayButton.DOWN, DisplayButton.CONFIRM, DisplayButton.BACK);
        RecordingWeatherProvider weather = new RecordingWeatherProvider(sampleWeather());
        AirportBrowserController controller = new AirportBrowserController(display, controls, List.of(), weather);

        controller.run();

        assertEquals(1, display.frames.size());
        assertFalse(weather.called);
    }

    private static List<NearbyAirport> airports() {
        return List.of(
                new NearbyAirport("SBGL", "GIG", "Galeao", "Rio de Janeiro", "BR", "large_airport",
                        new GeoPoint(-22.8090, -43.2506), 10, 0, 9.0),
                new NearbyAirport("SBSP", "CGH", "Congonhas", "Sao Paulo", "BR", "large_airport",
                        new GeoPoint(-23.6261, -46.6566), 350, 270, 802.0));
    }

    private static WeatherConditions sampleWeather() {
        return new WeatherConditions(
                Instant.parse("2026-09-09T12:00:00Z"), 25.0, 26.0, 60, 0.0, 1,
                true, 0.5, 20, 1015.0, 10.0, 90.0);
    }

    private static final class RecordingDisplay implements Display {
        private final List<RgbFrame> frames = new ArrayList<>();

        @Override
        public DisplayGeometry geometry() {
            return new DisplayGeometry(320, 240);
        }

        @Override
        public void show(RgbFrame frame) {
            frames.add(frame);
        }

        @Override
        public void close() {
        }
    }

    private static final class QueueControls implements DisplayControls {
        private final ArrayDeque<DisplayButtonEvent> events = new ArrayDeque<>();

        private QueueControls(DisplayButton... buttons) {
            for (DisplayButton button : buttons) {
                events.add(new DisplayButtonEvent(button, 0));
            }
        }

        @Override
        public Optional<DisplayButtonEvent> pollButton() {
            return Optional.ofNullable(events.poll());
        }
    }

    private static final class RecordingWeatherProvider implements WeatherProvider {
        private final WeatherConditions conditions;
        private boolean called;
        private GeoPoint requestedLocation;

        private RecordingWeatherProvider(WeatherConditions conditions) {
            this.conditions = conditions;
        }

        @Override
        public WeatherConditions currentConditions(GeoPoint location) {
            called = true;
            requestedLocation = location;
            return conditions;
        }
    }
}
