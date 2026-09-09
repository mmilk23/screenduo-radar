package io.github.mmilk23.screenduo.navigation;

import static io.github.mmilk23.screenduo.display.DisplayButton.*;
import static org.junit.jupiter.api.Assertions.*;

import io.github.mmilk23.screenduo.aircraft.NearbyAircraft;
import io.github.mmilk23.screenduo.aircraft.display.AircraftListScreenData;
import io.github.mmilk23.screenduo.aircraft.display.AircraftListScreenRenderer;
import io.github.mmilk23.screenduo.airport.NearbyAirport;
import io.github.mmilk23.screenduo.clock.display.AirportClockScreenData;
import io.github.mmilk23.screenduo.clock.display.AirportClockScreenRenderer;
import io.github.mmilk23.screenduo.config.ApplicationConfig;
import io.github.mmilk23.screenduo.display.*;
import io.github.mmilk23.screenduo.flight.*;
import io.github.mmilk23.screenduo.flight.display.*;
import io.github.mmilk23.screenduo.location.GeoPoint;
import io.github.mmilk23.screenduo.weather.WeatherConditions;
import io.github.mmilk23.screenduo.weather.display.WeatherScreenData;
import io.github.mmilk23.screenduo.weather.display.WeatherScreenRenderer;
import java.io.IOException;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class RadarDashboardControllerTest {
    private static final Instant NOW = Instant.parse("2026-09-07T12:00:00Z");
    private static final GeoPoint HOME = new GeoPoint(-22.9, -43.2);
    private static final DisplayGeometry GEOMETRY = new DisplayGeometry(320, 240);
    private static final WeatherConditions WEATHER = new WeatherConditions(
            NOW, 24.0, 25.0, 60, 0.0, 0, true, 0.5, 0, 1013.0, 12.0, 90.0);
    private static final FlightRoute ROUTE = new FlightRoute("SBGR", "SBRJ",
            NOW.minusSeconds(3600), NOW.plusSeconds(3600));

    @Test
    void integratesShortcutsWeatherAircraftAndAirportBoardPreservingSelections() throws Exception {
        Harness h = new Harness();
        h.run(ACTION_1, CONFIRM, DOWN, CONFIRM, ACTION_2, DOWN, CONFIRM,
                RIGHT, RIGHT, LEFT, BACK, BACK, RIGHT, BACK, ACTION_1,
                CONFIRM, CONFIRM, BACK, BACK, BACK);

        assertEquals(List.of(HOME, h.airports.get(1).position()), h.weatherLocations);
        assertEquals(1, h.aircraftQueries);
        assertEquals(1, h.airportQueries);
        assertEquals(List.of("TAM2"), h.routeCallsigns);
        assertEquals(List.of("SBRJ"), h.boardAirports);
        assertEquals(LocalDate.of(2026, 9, 7), h.boardDate);
        assertContains(h.frames, new DashboardScreenRenderer().withControls(
                new AircraftListScreenRenderer().render(GEOMETRY,
                        new AircraftListScreenData(h.aircraft, 1, NOW.plusMillis(700))),
                GEOMETRY, "UP DOWN  OK DETAILS  BACK WEATHER"));
        for (var direction : AirportFlightBoardData.Direction.values()) {
            assertContains(h.frames, new DashboardScreenRenderer().withControls(
                    new AirportFlightBoardRenderer().render(GEOMETRY,
                            new AirportFlightBoardData("SBRJ", h.boardDate, direction, h.flights, 0)),
                    GEOMETRY, "UP DOWN  LEFT ARR  RIGHT DEP  BACK"));
        }
    }

    @Test
    void emptyListsIgnoreSelectionActionsAndGlobalButtonsStillWork() throws Exception {
        Harness h = new Harness();
        h.aircraft = List.of();
        h.airports = List.of();
        h.run(ACTION_1, CONFIRM, UP, DOWN, CONFIRM, ACTION_2, CONFIRM, RIGHT, ACTION_1, BACK, BACK);

        assertEquals(1, h.aircraftQueries);
        assertEquals(1, h.airportQueries);
        assertTrue(h.routeCallsigns.isEmpty());
        assertTrue(h.boardAirports.isEmpty());
    }

    @Test
    void boardFailureSupportsRetryAndBackToAirportWeather() throws Exception {
        Harness h = new Harness();
        h.boardFailures = 1;
        h.run(ACTION_2, CONFIRM, RIGHT, CONFIRM, BACK, BACK, ACTION_1, BACK, BACK);

        assertEquals(List.of("SBGR", "SBGR"), h.boardAirports);
        assertEquals(2, h.weatherLocations.size());
        assertContains(h.frames, new DashboardScreenRenderer().withControls(
                new DashboardScreenRenderer().error("AIRPORT FLIGHTS ERROR"),
                GEOMETRY, "OK RETRY  BACK RETURN"));
    }

    @Test
    void globalButtonsEscapeDataErrorsAndAircraftQueryCanBeRetried() throws Exception {
        Harness h = new Harness();
        h.aircraftFailures = 1;
        h.run(ACTION_1, CONFIRM, ACTION_2, ACTION_1, CONFIRM, BACK, BACK);

        assertEquals(2, h.aircraftQueries);
        assertEquals(1, h.airportQueries);
    }

    @Test
    void routeFailureDoesNotBlockDetailsOrAirportShortcut() throws Exception {
        Harness h = new Harness();
        h.routeFails = true;
        h.run(ACTION_1, CONFIRM, CONFIRM, ACTION_2, ACTION_1, CONFIRM, CONFIRM, BACK, BACK, BACK);
        assertEquals(List.of("TAM1"), h.routeCallsigns);
        assertEquals(1, h.airportQueries);
    }

    @Test
    void debouncePersistsAcrossViewTransitions() throws Exception {
        Harness h = new Harness();
        h.stepMillis = 100;
        h.run(ACTION_1, CONFIRM, CONFIRM, UNKNOWN, CONFIRM, BACK, UNKNOWN, BACK, UNKNOWN, BACK);
        assertEquals(List.of("TAM1"), h.routeCallsigns);
        assertEquals(1, h.aircraftQueries);
    }

    @Test
    void boardUsesNewAirportAfterSelectionChangesAndTabsHandleEmptyLists() throws Exception {
        Harness h = new Harness();
        h.run(ACTION_2, RIGHT, RIGHT, UP, BACK, DOWN, RIGHT, RIGHT, DOWN, LEFT,
                BACK, ACTION_1, BACK, BACK);
        assertEquals(List.of("SBGR", "SBRJ"), h.boardAirports);
    }

    @Test
    void weatherFailureCanRetryAndCanExit() throws Exception {
        Harness h = new Harness();
        h.weatherFailures = 1;
        h.run(ACTION_1, CONFIRM, BACK, BACK);
        assertEquals(2, h.weatherLocations.size());
    }

    @Test
    void interruptionStopsNavigationWithoutBeingConvertedToDataError() {
        Harness h = new Harness();
        h.interruptWeather = true;
        assertThrows(InterruptedException.class, () -> h.run(ACTION_1));
        assertEquals(1, h.frames.size());
    }

    @Test
    void keepsBoardPageSelectionAcrossTabSwitchesAndReturningFromAirportList() throws Exception {
        Harness h = new Harness();
        h.flights = java.util.stream.IntStream.rangeClosed(1, 6)
                .mapToObj(index -> new ScheduledFlight("TAM" + index, new FlightRoute("SBRJ", "SBGR",
                        NOW.plusSeconds(index * 600L), NOW.plusSeconds(index * 600L + 3600))))
                .toList();
        h.run(ACTION_2, RIGHT, UP, RIGHT, UP, LEFT, BACK, RIGHT, ACTION_1, BACK, BACK);
        var expected = new DashboardScreenRenderer().withControls(
                new AirportFlightBoardRenderer().render(GEOMETRY,
                        new AirportFlightBoardData("SBGR", LocalDate.of(2026, 9, 7),
                                AirportFlightBoardData.Direction.ARRIVALS, h.flights, 5)),
                GEOMETRY, "UP DOWN  LEFT ARR  RIGHT DEP  BACK");
        assertEquals(3, h.frames.stream()
                .filter(frame -> Arrays.equals(frame.pixels(), expected.pixels())).count());
        assertEquals(List.of("SBGR"), h.boardAirports);
    }

    @Test
    void confirmImmediatelyAfterButtonOneOpensAircraftWithoutBeingDebounced() throws Exception {
        Harness h = new Harness();
        h.stepMillis = 100;
        h.run(ACTION_2, ACTION_1, CONFIRM, BACK, UNKNOWN, BACK);
        assertEquals(1, h.aircraftQueries);
        assertContains(h.frames, new DashboardScreenRenderer().withControls(
                new AircraftListScreenRenderer().render(GEOMETRY,
                        new AircraftListScreenData(h.aircraft, 0, NOW.plusMillis(300))),
                GEOMETRY, "UP DOWN  OK DETAILS  BACK WEATHER"));
    }


    @Test
    void startsOnClockAndDoubleButtonOneReturnsToClock() throws Exception {
        Harness h = new Harness();
        h.run(ACTION_1, UNKNOWN, ACTION_1, BACK);

        assertEquals(1, h.weatherLocations.size());
        assertContains(h.frames, new DashboardScreenRenderer().withControls(
                new AirportClockScreenRenderer().render(GEOMETRY,
                        new AirportClockScreenData("RIO", WEATHER, NOW, ZoneOffset.UTC)),
                GEOMETRY, "OK AIRCRAFT  1 WEATHER  2 AIRPORTS"));
    }

    @Test
    void holdingButtonOneDoesNotToggleBackToClock() throws Exception {
        Harness h = new Harness();
        h.run(ACTION_1, ACTION_1, ACTION_1, BACK, BACK);

        assertEquals(1, h.weatherLocations.size());
        assertContains(h.frames, new DashboardScreenRenderer().withControls(
                new WeatherScreenRenderer().render(GEOMETRY, new WeatherScreenData("RIO", WEATHER)),
                GEOMETRY, "OK AIRCRAFT  1 CLOCK  BACK"));
    }

    @Test
    void clockConfirmOpensNearbyAircraftAndBackKeepsClockAlive() throws Exception {
        Harness h = new Harness();
        h.run(BACK, CONFIRM, BACK, BACK);

        assertEquals(1, h.aircraftQueries);
        assertContains(h.frames, new DashboardScreenRenderer().withControls(
                new AirportClockScreenRenderer().render(GEOMETRY,
                        new AirportClockScreenData("RIO", WEATHER, NOW, ZoneOffset.UTC)),
                GEOMETRY, "OK AIRCRAFT  1 WEATHER  2 AIRPORTS"));
        assertContains(h.frames, new DashboardScreenRenderer().withControls(
                new AircraftListScreenRenderer().render(GEOMETRY,
                        new AircraftListScreenData(h.aircraft, 0, NOW.plusMillis(1400))),
                GEOMETRY, "UP DOWN  OK DETAILS  BACK WEATHER"));
    }
    private static void assertContains(List<RgbFrame> frames, RgbFrame expected) {
        assertTrue(frames.stream().anyMatch(frame -> Arrays.equals(frame.pixels(), expected.pixels())),
                "Expected screen was not displayed");
    }

    private static final class Harness extends Clock implements Display {
        private Instant now = NOW;
        private long stepMillis = 700;
        private final List<RgbFrame> frames = new ArrayList<>();
        private final List<GeoPoint> weatherLocations = new ArrayList<>();
        private final List<String> routeCallsigns = new ArrayList<>();
        private final List<String> boardAirports = new ArrayList<>();
        private LocalDate boardDate;
        private int aircraftQueries;
        private int airportQueries;
        private int aircraftFailures;
        private int boardFailures;
        private int weatherFailures;
        private boolean routeFails;
        private boolean interruptWeather;
        private List<NearbyAirport> airports = List.of(
                airport("SBGR", new GeoPoint(-23.43, -46.47), 10),
                airport("SBRJ", new GeoPoint(-22.91, -43.16), 20));
        private List<NearbyAircraft> aircraft = List.of(plane("TAM1", 5), plane("TAM2", 10));
        private List<ScheduledFlight> flights = List.of(new ScheduledFlight("TAM1", ROUTE));

        void run(DisplayButton... buttons) throws InterruptedException {
            Iterator<DisplayButton> iterator = List.of(buttons).iterator();
            AtomicBoolean exhausted = new AtomicBoolean();
            DisplayControls controls = () -> {
                if (!iterator.hasNext()) {
                    exhausted.set(true);
                    Thread.currentThread().interrupt();
                    return Optional.empty();
                }
                now = now.plusMillis(stepMillis);
                return Optional.of(new DisplayButtonEvent(iterator.next(), 0));
            };
            RadarDashboardController controller = new RadarDashboardController(this, controls,
                    new ApplicationConfig(HOME, "RIO", 50, 100),
                    point -> {
                        weatherLocations.add(point);
                        if (interruptWeather) {
                            throw new InterruptedException();
                        }
                        if (weatherFailures-- > 0) {
                            throw new IOException("Weather offline");
                        }
                        return WEATHER;
                    },
                    (point, radius) -> {
                        assertEquals(HOME, point);
                        assertEquals(50.0, radius);
                        aircraftQueries++;
                        if (aircraftFailures-- > 0) {
                            throw new IOException("Aircraft offline");
                        }
                        return aircraft;
                    },
                    (point, radius) -> {
                        assertEquals(HOME, point);
                        assertEquals(100.0, radius);
                        airportQueries++;
                        return airports;
                    },
                    (callsign, time) -> {
                        routeCallsigns.add(callsign);
                        if (routeFails) {
                            throw new IOException("Route offline");
                        }
                        return Optional.of(ROUTE);
                    },
                    (airport, date) -> {
                        boardAirports.add(airport);
                        boardDate = date;
                        if (boardFailures-- > 0) {
                            throw new IOException("Board offline");
                        }
                        return flights;
                    }, this);
            try {
                controller.run();
            } catch (InterruptedException exception) {
                if (!exhausted.get()) {
                    throw exception;
                }
                Thread.interrupted();
            }
            assertFalse(iterator.hasNext(), "Dashboard exited before handling every button");
        }

        @Override public DisplayGeometry geometry() { return GEOMETRY; }
        @Override public void show(RgbFrame frame) { frames.add(frame); }
        @Override public void close() { }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(now, zone); }
        @Override public Instant instant() { return now; }

        private static NearbyAirport airport(String code, GeoPoint point, double distance) {
            return new NearbyAirport(code, "", code, code, "BR", "large_airport",
                    point, distance, 45.0, 10.0);
        }

        private static NearbyAircraft plane(String callsign, double distance) {
            return new NearbyAircraft(callsign, callsign, "TAM", "LATAM", "Brazil",
                    HOME, distance, 45.0, 1000.0, 400.0, 90.0, false);
        }
    }
}

