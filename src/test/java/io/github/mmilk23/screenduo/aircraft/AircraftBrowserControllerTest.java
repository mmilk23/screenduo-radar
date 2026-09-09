package io.github.mmilk23.screenduo.aircraft;

import static io.github.mmilk23.screenduo.display.DisplayButton.*;
import static org.junit.jupiter.api.Assertions.*;

import io.github.mmilk23.screenduo.aircraft.display.AircraftDetailScreenData;
import io.github.mmilk23.screenduo.aircraft.display.AircraftDetailScreenRenderer;
import io.github.mmilk23.screenduo.flight.FlightRoute;
import io.github.mmilk23.screenduo.flight.FlightRouteProvider;
import io.github.mmilk23.screenduo.aircraft.display.AircraftListScreenData;
import io.github.mmilk23.screenduo.aircraft.display.AircraftListScreenRenderer;
import io.github.mmilk23.screenduo.display.*;
import io.github.mmilk23.screenduo.location.GeoPoint;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class AircraftBrowserControllerTest {

    private static final GeoPoint LOCATION = new GeoPoint(-22.9, -43.2);
    private static final Instant LOADED_AT = Instant.parse("2026-09-07T12:34:00Z");

    @Test
    void navigatesPagesWrapsDebouncesAndReturnsToSelectedAircraft() throws InterruptedException {
        RecordingDisplay display = new RecordingDisplay();
        TestClock clock = new TestClock();
        List<NearbyAircraft> sorted = List.of(
                aircraft(1), aircraft(2), aircraft(3), aircraft(4), aircraft(5));
        AtomicInteger queries = new AtomicInteger();
        AircraftProvider provider = (center, radius) -> {
            assertEquals(LOCATION, center);
            assertEquals(50.0, radius);
            queries.incrementAndGet();
            return sorted.reversed();
        };
        DisplayControls controls = controls(clock, 100,
                UP, DOWN, DOWN, UNKNOWN, DOWN, CONFIRM, BACK, BACK, UNKNOWN, BACK);

        new AircraftBrowserController(display, controls, provider, LOCATION, 50, clock).run();

        assertEquals(1, queries.get());
        assertEquals(8, display.frames.size());
        assertList(display, 1, sorted, 0);
        assertList(display, 2, sorted, 4);
        assertList(display, 3, sorted, 0);
        assertList(display, 4, sorted, 1);
        assertArrayEquals(new AircraftDetailScreenRenderer().render(display.geometry(),
                new AircraftDetailScreenData(sorted.get(1), LOADED_AT, Optional.empty())).pixels(),
                display.frames.get(6).pixels());
        assertArrayEquals(display.frames.get(4).pixels(), display.frames.get(7).pixels());
    }

    @Test
    void acceptsHeldButtonAfterDebounceInterval() throws InterruptedException {
        RecordingDisplay display = new RecordingDisplay();
        TestClock clock = new TestClock();
        List<NearbyAircraft> aircraft = List.of(aircraft(1), aircraft(2), aircraft(3));
        new AircraftBrowserController(display, controls(clock, 600, DOWN, DOWN, BACK),
                (center, radius) -> aircraft, LOCATION, 50, clock).run();

        assertEquals(4, display.frames.size());
        assertList(display, 3, aircraft, 2);
    }

    @Test
    void emptyListIgnoresNavigationAndConfirmButAllowsExit() throws InterruptedException {
        RecordingDisplay display = new RecordingDisplay();
        TestClock clock = new TestClock();
        new AircraftBrowserController(display, controls(clock, 100, UP, DOWN, CONFIRM, BACK),
                (center, radius) -> List.of(), LOCATION, 50, clock).run();

        assertEquals(2, display.frames.size());
        assertList(display, 1, List.of(), 0);
    }

    @Test
    void retriesFailedQueryOnConfirm() throws InterruptedException {
        RecordingDisplay display = new RecordingDisplay();
        TestClock clock = new TestClock();
        AtomicInteger queries = new AtomicInteger();
        AircraftProvider provider = (center, radius) -> {
            if (queries.incrementAndGet() == 1) {
                throw new IOException("HTTP 429");
            }
            return List.of(aircraft(1));
        };
        new AircraftBrowserController(display, controls(clock, 100, CONFIRM, BACK),
                provider, LOCATION, 50, clock).run();

        assertEquals(2, queries.get());
        assertEquals(4, display.frames.size());
        assertArrayEquals(new AircraftListScreenRenderer().renderError(display.geometry()).pixels(),
                display.frames.get(1).pixels());
        assertArrayEquals(new AircraftListScreenRenderer().render(display.geometry(),
                new AircraftListScreenData(List.of(aircraft(1)), 0, LOADED_AT.plusMillis(100))).pixels(),
                display.frames.get(3).pixels());
    }

    @Test
    void exitsErrorScreenWithoutRetrying() throws InterruptedException {
        RecordingDisplay display = new RecordingDisplay();
        TestClock clock = new TestClock();
        AtomicInteger queries = new AtomicInteger();
        new AircraftBrowserController(display, controls(clock, 100, DOWN, BACK),
                (center, radius) -> {
                    queries.incrementAndGet();
                    throw new IOException("Offline");
                }, LOCATION, 50, clock).run();

        assertEquals(1, queries.get());
        assertEquals(2, display.frames.size());
    }

    @Test
    void propagatesInterruptedQueryWithoutPollingOrShowingDataError() {
        RecordingDisplay display = new RecordingDisplay();
        AircraftBrowserController controller = new AircraftBrowserController(
                display, () -> { throw new AssertionError("Must not poll after interruption"); },
                (center, radius) -> { throw new InterruptedException("Cancelled"); },
                LOCATION, 50);
        assertThrows(InterruptedException.class, controller::run);
        assertEquals(1, display.frames.size());
    }

    @Test
    void loadsRouteOnlyOnDetailsAndCachesItAcrossBackAndConfirm() throws InterruptedException {
        RecordingDisplay display = new RecordingDisplay();
        TestClock clock = new TestClock();
        AtomicInteger queries = new AtomicInteger();
        FlightRoute route = new FlightRoute("SBGR", "SBRJ",
                LOADED_AT.minusSeconds(1800), LOADED_AT.plusSeconds(1800));
        FlightRouteProvider routeProvider = (callsign, reference) -> {
            assertEquals("TAM1", callsign);
            assertEquals(LOADED_AT, reference);
            assertEquals(3, display.frames.size(), "List and loading screen must already be visible");
            queries.incrementAndGet();
            return Optional.of(route);
        };
        new AircraftBrowserController(display,
                controls(clock, 100, CONFIRM, BACK, CONFIRM, BACK, UNKNOWN, BACK),
                (center, radius) -> List.of(aircraft(1)), routeProvider, LOCATION, 50, clock).run();

        assertEquals(1, queries.get());
        assertEquals(7, display.frames.size());
        assertArrayEquals(new AircraftDetailScreenRenderer().render(display.geometry(),
                new AircraftDetailScreenData(aircraft(1), LOADED_AT, Optional.of(route))).pixels(),
                display.frames.get(3).pixels());
        assertArrayEquals(display.frames.get(3).pixels(), display.frames.get(5).pixels());
        assertArrayEquals(display.frames.get(1).pixels(), display.frames.get(6).pixels());
    }

    @Test
    void routeFailurePreservesDetailsAndNavigationWithoutRepeatedRequests() throws InterruptedException {
        RecordingDisplay display = new RecordingDisplay();
        TestClock clock = new TestClock();
        AtomicInteger queries = new AtomicInteger();
        new AircraftBrowserController(display,
                controls(clock, 100, CONFIRM, BACK, CONFIRM, BACK, UNKNOWN, BACK),
                (center, radius) -> List.of(aircraft(1)),
                (callsign, reference) -> {
                    queries.incrementAndGet();
                    throw new IOException("SIROS unavailable");
                }, LOCATION, 50, clock).run();

        assertEquals(1, queries.get());
        assertEquals(7, display.frames.size());
        assertArrayEquals(new AircraftDetailScreenRenderer().render(display.geometry(),
                new AircraftDetailScreenData(aircraft(1), LOADED_AT, Optional.empty())).pixels(),
                display.frames.get(3).pixels());
    }

    @Test
    void propagatesRouteInterruption() {
        RecordingDisplay display = new RecordingDisplay();
        TestClock clock = new TestClock();
        AircraftBrowserController controller = new AircraftBrowserController(
                display, controls(clock, 100, CONFIRM),
                (center, radius) -> List.of(aircraft(1)),
                (callsign, reference) -> { throw new InterruptedException("Cancelled"); },
                LOCATION, 50, clock);

        assertThrows(InterruptedException.class, controller::run);
        assertEquals(3, display.frames.size());
    }

    private static void assertList(
            RecordingDisplay display, int frame, List<NearbyAircraft> aircraft, int selection) {
        assertArrayEquals(new AircraftListScreenRenderer().render(display.geometry(),
                new AircraftListScreenData(aircraft, selection, LOADED_AT)).pixels(),
                display.frames.get(frame).pixels());
    }

    private static DisplayControls controls(TestClock clock, long stepMillis, DisplayButton... buttons) {
        AtomicInteger index = new AtomicInteger();
        return () -> {
            int next = index.getAndIncrement();
            assertTrue(next < buttons.length, "Browser should have exited");
            clock.now = clock.now.plusMillis(stepMillis);
            return Optional.of(new DisplayButtonEvent(buttons[next], 0));
        };
    }

    private static NearbyAircraft aircraft(int index) {
        return new NearbyAircraft("abc00" + index, "TAM" + index, "TAM", "LATAM", "Brazil",
                LOCATION, index * 5.0, 45.0, 1000.0, 400.0, 90.0, false);
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

    private static final class TestClock extends Clock {
        private Instant now = LOADED_AT;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}