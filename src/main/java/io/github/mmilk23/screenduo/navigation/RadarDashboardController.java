package io.github.mmilk23.screenduo.navigation;

import io.github.mmilk23.screenduo.aircraft.AircraftProvider;
import io.github.mmilk23.screenduo.aircraft.NearbyAircraft;
import io.github.mmilk23.screenduo.airline.logo.AirlineLogoProvider;
import io.github.mmilk23.screenduo.aircraft.display.AircraftDetailScreenData;
import io.github.mmilk23.screenduo.aircraft.display.AircraftDetailScreenRenderer;
import io.github.mmilk23.screenduo.aircraft.display.AircraftListScreenData;
import io.github.mmilk23.screenduo.aircraft.display.AircraftListScreenRenderer;
import io.github.mmilk23.screenduo.airport.AirportProvider;
import io.github.mmilk23.screenduo.airport.NearbyAirport;
import io.github.mmilk23.screenduo.airport.display.AirportListScreenData;
import io.github.mmilk23.screenduo.airport.display.AirportListScreenRenderer;
import io.github.mmilk23.screenduo.clock.display.AirportClockScreenData;
import io.github.mmilk23.screenduo.clock.display.AirportClockScreenRenderer;
import io.github.mmilk23.screenduo.config.ApplicationConfig;
import io.github.mmilk23.screenduo.display.Display;
import io.github.mmilk23.screenduo.display.DisplayButton;
import io.github.mmilk23.screenduo.display.DisplayButtonEvent;
import io.github.mmilk23.screenduo.display.DisplayControls;
import io.github.mmilk23.screenduo.display.RgbFrame;
import io.github.mmilk23.screenduo.display.StatusScreenRenderer;
import io.github.mmilk23.screenduo.flight.AirportFlightProvider;
import io.github.mmilk23.screenduo.flight.FlightRoute;
import io.github.mmilk23.screenduo.flight.FlightRouteProvider;
import io.github.mmilk23.screenduo.flight.ScheduledFlight;
import io.github.mmilk23.screenduo.flight.display.AirportFlightBoardData;
import io.github.mmilk23.screenduo.flight.display.AirportFlightBoardData.Direction;
import io.github.mmilk23.screenduo.flight.display.AirportFlightBoardRenderer;
import io.github.mmilk23.screenduo.weather.WeatherProvider;
import io.github.mmilk23.screenduo.weather.display.WeatherScreenData;
import io.github.mmilk23.screenduo.weather.display.WeatherScreenRenderer;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.github.mmilk23.screenduo.navigation.DashboardScreenRenderer.LOGICAL_GEOMETRY;

/** Owns all integrated navigation; USB polling and rendering stay on the same thread. */
public final class RadarDashboardController {

    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);
    private static final Duration BUTTON_DEBOUNCE = Duration.ofMillis(600);

    private final Display display;
    private final DisplayControls controls;
    private final ApplicationConfig config;
    private final WeatherProvider weatherProvider;
    private final AircraftProvider aircraftProvider;
    private final AirportProvider airportProvider;
    private final FlightRouteProvider routeProvider;
    private final AirportFlightProvider airportFlightProvider;
    private final AirlineLogoProvider logoProvider;
    private final Clock clock;
    private final DashboardScreenRenderer chrome = new DashboardScreenRenderer();
    private final Map<String, WeatherScreenData> airportWeather = new HashMap<>();
    private final Map<String, Optional<FlightRoute>> routes = new HashMap<>();

    private View view = View.CLOCK;
    private View boardReturn = View.AIRPORT_LIST;
    private boolean failed;
    private boolean traceNavigation;
    private WeatherScreenData localWeather;
    private List<NearbyAircraft> aircraft;
    private List<NearbyAirport> airports;
    private Instant aircraftLoadedAt;
    private int aircraftIndex;
    private int airportIndex;
    private String boardAirport;
    private LocalDate boardDate;
    private List<ScheduledFlight> boardFlights;
    private Direction direction = Direction.ARRIVALS;
    private final Map<Direction, Integer> boardSelections = new HashMap<>();
    private boolean actionOneArmed;
    private long lastClockSecond = Long.MIN_VALUE;
    private DisplayButton lastButton;
    private Instant lastButtonAt = Instant.MIN;

    public RadarDashboardController(
            Display display, DisplayControls controls, ApplicationConfig config,
            WeatherProvider weatherProvider, AircraftProvider aircraftProvider,
            AirportProvider airportProvider, FlightRouteProvider routeProvider,
            AirportFlightProvider airportFlightProvider) {
        this(display, controls, config, weatherProvider, aircraftProvider, airportProvider,
                routeProvider, airportFlightProvider, AirlineLogoProvider.NONE, Clock.systemDefaultZone());
    }

    public RadarDashboardController(
            Display display, DisplayControls controls, ApplicationConfig config,
            WeatherProvider weatherProvider, AircraftProvider aircraftProvider,
            AirportProvider airportProvider, FlightRouteProvider routeProvider,
            AirportFlightProvider airportFlightProvider, AirlineLogoProvider logoProvider) {
        this(display, controls, config, weatherProvider, aircraftProvider, airportProvider,
                routeProvider, airportFlightProvider, logoProvider, Clock.systemDefaultZone());
    }

    RadarDashboardController(
            Display display, DisplayControls controls, ApplicationConfig config,
            WeatherProvider weatherProvider, AircraftProvider aircraftProvider,
            AirportProvider airportProvider, FlightRouteProvider routeProvider,
            AirportFlightProvider airportFlightProvider, Clock clock) {
        this(display, controls, config, weatherProvider, aircraftProvider, airportProvider,
                routeProvider, airportFlightProvider, AirlineLogoProvider.NONE, clock);
    }

    RadarDashboardController(
            Display display, DisplayControls controls, ApplicationConfig config,
            WeatherProvider weatherProvider, AircraftProvider aircraftProvider,
            AirportProvider airportProvider, FlightRouteProvider routeProvider,
            AirportFlightProvider airportFlightProvider, AirlineLogoProvider logoProvider, Clock clock) {
        this.display = display;
        this.controls = controls;
        this.config = config;
        this.weatherProvider = weatherProvider;
        this.aircraftProvider = aircraftProvider;
        this.airportProvider = airportProvider;
        this.routeProvider = routeProvider;
        this.airportFlightProvider = airportFlightProvider;
        this.logoProvider = logoProvider;
        this.clock = clock;
    }

    public void run() throws InterruptedException {
        run(false);
    }

    public void run(boolean traceNavigation) throws InterruptedException {
        this.traceNavigation = traceNavigation;
        open(View.CLOCK);
        System.out.println("Dashboard active. Button 1: weather. Button 2: airports.");
        boolean running = true;
        while (running && !Thread.currentThread().isInterrupted()) {
            Optional<DisplayButtonEvent> event = controls.pollButton();
            if (event.isEmpty()) {
                releaseMomentaryButtons();
            } else {
                DisplayButtonEvent input = event.orElseThrow();
                DisplayButton button = input.button();
                boolean accepted = button != DisplayButton.UNKNOWN && accept(button);
                if (traceNavigation) {
                    System.out.printf(java.util.Locale.ROOT,
                            "Navigation input: native=%d button=%s view=%s accepted=%s%n",
                            input.sourceCode(), button, view, accepted);
                }
                if (button == DisplayButton.UNKNOWN) {
                    releaseMomentaryButtons();
                } else if (accepted) {
                    running = handle(button);
                }
            }
            if (running && view == View.CLOCK && !failed
                    && clock.instant().getEpochSecond() != lastClockSecond) {
                render();
            }
            if (running) {
                Thread.sleep(POLL_INTERVAL.toMillis());
            }
        }
    }

    private void releaseMomentaryButtons() {
        lastButton = null;
    }

    private boolean accept(DisplayButton button) {
        if (isMomentaryShortcut(button) && button == lastButton) {
            return false;
        }
        Instant now = clock.instant();
        if (button == lastButton && now.isBefore(lastButtonAt.plus(BUTTON_DEBOUNCE))) {
            return false;
        }
        lastButton = button;
        lastButtonAt = now;
        return true;
    }

    private boolean handle(DisplayButton button) throws InterruptedException {
        if (button == DisplayButton.ACTION_1) {
            open(actionOneArmed ? View.CLOCK : View.LOCAL_WEATHER);
            actionOneArmed = !actionOneArmed;
            return true;
        }
        actionOneArmed = false;
        if (button == DisplayButton.ACTION_2) {
            open(View.AIRPORT_LIST);
            return true;
        }
        if (button == DisplayButton.BACK) {
            if (view == View.CLOCK) {
                return true;
            }
            open(switch (view) {
                case LOCAL_WEATHER, AIRCRAFT_LIST, AIRPORT_LIST -> View.CLOCK;
                case AIRCRAFT_DETAIL -> View.AIRCRAFT_LIST;
                case AIRPORT_WEATHER -> View.AIRPORT_LIST;
                case AIRPORT_FLIGHTS -> boardReturn;
                default -> View.CLOCK;
            });
            return true;
        }
        if (failed) {
            if (button == DisplayButton.CONFIRM) {
                open(view);
            }
            return true;
        }
        if (button == DisplayButton.RIGHT
                && (view == View.AIRPORT_LIST || view == View.AIRPORT_WEATHER)
                && !airports.isEmpty()) {
            boardReturn = view;
            open(View.AIRPORT_FLIGHTS);
            return true;
        }
        switch (view) {
            case CLOCK -> {
                if (button == DisplayButton.CONFIRM) {
                    open(View.AIRCRAFT_LIST);
                }
            }
            case LOCAL_WEATHER -> {
                if (button == DisplayButton.CONFIRM) {
                    open(View.AIRCRAFT_LIST);
                }
            }
            case AIRCRAFT_LIST -> {
                if (!aircraft.isEmpty()) {
                    if (button == DisplayButton.CONFIRM) {
                        open(View.AIRCRAFT_DETAIL);
                    } else if (isMove(button)) {
                        aircraftIndex = move(aircraftIndex, button, aircraft.size());
                        render();
                    }
                }
            }
            case AIRPORT_LIST -> {
                if (!airports.isEmpty()) {
                    if (button == DisplayButton.CONFIRM) {
                        open(View.AIRPORT_WEATHER);
                    } else if (isMove(button)) {
                        airportIndex = move(airportIndex, button, airports.size());
                        render();
                    }
                }
            }
            case AIRPORT_FLIGHTS -> {
                if (button == DisplayButton.LEFT || button == DisplayButton.RIGHT
                        || button == DisplayButton.CONFIRM) {
                    direction = button == DisplayButton.LEFT ? Direction.ARRIVALS
                            : button == DisplayButton.RIGHT ? Direction.DEPARTURES
                            : direction == Direction.ARRIVALS ? Direction.DEPARTURES : Direction.ARRIVALS;
                    render();
                } else if (isMove(button) && !boardData().flights().isEmpty()) {
                    boardSelections.put(direction,
                            move(boardSelections.getOrDefault(direction, 0), button, boardData().flights().size()));
                    render();
                }
            }
            default -> { }
        }
        return true;
    }

    private void open(View target) throws InterruptedException {
        if (traceNavigation) {
            System.out.printf(java.util.Locale.ROOT, "Navigation view: %s -> %s%n", view, target);
        }
        view = target;
        failed = false;
        try {
            switch (view) {
                case CLOCK -> {
                    loadLocalWeather();
                }
                case LOCAL_WEATHER -> {
                    if (localWeather == null) {
                        loading("LOADING WEATHER");
                        localWeather = new WeatherScreenData(config.city(),
                                weatherProvider.currentConditions(config.location()));
                    }
                }
                case AIRCRAFT_LIST -> {
                    if (aircraft == null) {
                        loading("LOADING AIRCRAFT");
                        aircraft = aircraftProvider.findNearby(config.location(), config.aircraftRadiusKm())
                                .stream().sorted(Comparator.comparingDouble(NearbyAircraft::distanceKm)).toList();
                        aircraftLoadedAt = clock.instant();
                    }
                }
                case AIRPORT_LIST -> {
                    if (airports == null) {
                        loading("LOADING AIRPORTS");
                        airports = airportProvider.findNearby(config.location(), config.airportRadiusKm())
                                .stream().sorted(Comparator.comparingDouble(NearbyAirport::distanceKm)).toList();
                    }
                }
                case AIRPORT_WEATHER -> {
                    NearbyAirport airport = airports.get(airportIndex);
                    if (!airportWeather.containsKey(airport.ident())) {
                        loading("LOADING WEATHER");
                        String code = airport.iataCode().isBlank() ? airport.ident() : airport.iataCode();
                        airportWeather.put(airport.ident(), new WeatherScreenData(
                                code + " " + airport.municipality(),
                                weatherProvider.currentConditions(airport.position())));
                    }
                }
                case AIRCRAFT_DETAIL -> loadRoute();
                case AIRPORT_FLIGHTS -> loadBoard();
            }
        } catch (IOException exception) {
            failed = true;
            System.err.println("Unable to load " + view + ": " + exception.getMessage());
        }
        render();
    }

    private void loadLocalWeather() throws IOException, InterruptedException {
        if (localWeather == null) {
            loading("LOADING WEATHER");
            localWeather = new WeatherScreenData(config.city(),
                    weatherProvider.currentConditions(config.location()));
        }
    }

    private void loadRoute() throws InterruptedException {
        String callsign = aircraft.get(aircraftIndex).callsign();
        if (!routes.containsKey(callsign)) {
            loading("LOADING ROUTE");
            try {
                routes.put(callsign, routeProvider.findRoute(callsign, aircraftLoadedAt));
            } catch (IOException exception) {
                routes.put(callsign, Optional.empty());
                System.err.println("Unable to load planned route: " + exception.getMessage());
            }
        }
    }

    private void loadBoard() throws IOException, InterruptedException {
        String code = airports.get(airportIndex).ident();
        LocalDate today = clock.instant().atZone(ZoneOffset.UTC).toLocalDate();
        if (boardFlights == null || !code.equals(boardAirport) || !today.equals(boardDate)) {
            loading("LOADING FLIGHTS");
            List<ScheduledFlight> loaded = airportFlightProvider.findFlights(code, today);
            boardFlights = List.copyOf(loaded);
            boardAirport = code;
            boardDate = today;
            direction = Direction.ARRIVALS;
            boardSelections.clear();
        }
    }

    private void loading(String message) {
        display.show(new StatusScreenRenderer().render(display.geometry(), "RADAR", message));
    }

    private AirportFlightBoardData boardData() {
        return new AirportFlightBoardData(boardAirport, boardDate, direction,
                boardFlights, boardSelections.getOrDefault(direction, 0));
    }

    private void render() {
        RgbFrame frame;
        String hints;
        if (failed) {
            frame = chrome.error(switch (view) {
                case AIRPORT_FLIGHTS -> "AIRPORT FLIGHTS ERROR";
                case AIRPORT_LIST -> "AIRPORTS ERROR";
                case AIRCRAFT_LIST -> "AIRCRAFT ERROR";
                default -> "WEATHER ERROR";
            });
            hints = "OK RETRY  BACK RETURN";
        } else {
            frame = switch (view) {
                case CLOCK -> new AirportClockScreenRenderer().render(LOGICAL_GEOMETRY,
                        new AirportClockScreenData(config.city(), localWeather.conditions(),
                                clock.instant(), clock.getZone()));
                case LOCAL_WEATHER -> new WeatherScreenRenderer().render(LOGICAL_GEOMETRY, localWeather);
                case AIRPORT_WEATHER -> new WeatherScreenRenderer().render(
                        LOGICAL_GEOMETRY, airportWeather.get(airports.get(airportIndex).ident()));
                case AIRPORT_LIST -> new AirportListScreenRenderer().render(
                        LOGICAL_GEOMETRY, new AirportListScreenData(airports, airportIndex));
                case AIRCRAFT_LIST -> new AircraftListScreenRenderer().render(
                        LOGICAL_GEOMETRY, new AircraftListScreenData(
                                aircraft, aircraftIndex, aircraftLoadedAt, airlineLogos(aircraft)));
                case AIRCRAFT_DETAIL -> new AircraftDetailScreenRenderer().render(LOGICAL_GEOMETRY,
                        new AircraftDetailScreenData(aircraft.get(aircraftIndex), aircraftLoadedAt,
                                routes.get(aircraft.get(aircraftIndex).callsign()),
                                logoProvider.findLogo(aircraft.get(aircraftIndex).airlineIcaoCode())));
                case AIRPORT_FLIGHTS -> new AirportFlightBoardRenderer().render(LOGICAL_GEOMETRY, boardData());
            };
            hints = switch (view) {
                case CLOCK -> "OK AIRCRAFT  1 WEATHER  2 AIRPORTS";
                case LOCAL_WEATHER -> "OK AIRCRAFT  1 CLOCK  BACK";
                case AIRCRAFT_LIST -> "UP DOWN  OK DETAILS  BACK WEATHER";
                case AIRCRAFT_DETAIL -> "BACK AIRCRAFT";
                case AIRPORT_LIST -> "UP DOWN  OK WEATHER  RIGHT FLIGHTS  BACK";
                case AIRPORT_WEATHER -> "RIGHT FLIGHTS  BACK AIRPORTS";
                case AIRPORT_FLIGHTS -> "UP DOWN  LEFT ARR  RIGHT DEP  BACK";
            };
        }
        display.show(chrome.withControls(frame, display.geometry(), hints));
        if (view == View.CLOCK && !failed) {
            lastClockSecond = clock.instant().getEpochSecond();
        }
        if (traceNavigation) {
            System.out.printf(java.util.Locale.ROOT,
                    "Navigation frame sent: view=%s failed=%s%n", view, failed);
        }
    }

    private Map<String, io.github.mmilk23.screenduo.display.RgbFrame> airlineLogos(List<NearbyAircraft> items) {
        Map<String, io.github.mmilk23.screenduo.display.RgbFrame> logos = new HashMap<>();
        int pageStart = aircraftIndex / AircraftListScreenRenderer.PAGE_SIZE * AircraftListScreenRenderer.PAGE_SIZE;
        int pageEnd = Math.min(pageStart + AircraftListScreenRenderer.PAGE_SIZE, items.size());
        for (int index = pageStart; index < pageEnd; index++) {
            NearbyAircraft item = items.get(index);
            logoProvider.findLogo(item.airlineIcaoCode()).ifPresent(logo -> logos.put(item.airlineIcaoCode(), logo));
        }
        return logos;
    }

    private static boolean isMove(DisplayButton button) {
        return button == DisplayButton.UP || button == DisplayButton.DOWN;
    }

    private static boolean isMomentaryShortcut(DisplayButton button) {
        return button == DisplayButton.ACTION_1 || button == DisplayButton.ACTION_2;
    }

    private static int move(int index, DisplayButton button, int count) {
        return Math.floorMod(index + (button == DisplayButton.UP ? -1 : 1), count);
    }

    private enum View {
        CLOCK, LOCAL_WEATHER, AIRCRAFT_LIST, AIRCRAFT_DETAIL,
        AIRPORT_LIST, AIRPORT_WEATHER, AIRPORT_FLIGHTS
    }
}
