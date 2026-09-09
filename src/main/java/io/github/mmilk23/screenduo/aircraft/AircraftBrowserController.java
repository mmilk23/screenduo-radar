package io.github.mmilk23.screenduo.aircraft;

import io.github.mmilk23.screenduo.aircraft.display.AircraftDetailScreenData;
import io.github.mmilk23.screenduo.airline.logo.AirlineLogoProvider;
import io.github.mmilk23.screenduo.aircraft.display.AircraftDetailScreenRenderer;
import io.github.mmilk23.screenduo.flight.FlightRoute;
import io.github.mmilk23.screenduo.flight.FlightRouteProvider;
import io.github.mmilk23.screenduo.aircraft.display.AircraftListScreenData;
import io.github.mmilk23.screenduo.aircraft.display.AircraftListScreenRenderer;
import io.github.mmilk23.screenduo.display.Display;
import io.github.mmilk23.screenduo.display.DisplayButton;
import io.github.mmilk23.screenduo.display.DisplayButtonEvent;
import io.github.mmilk23.screenduo.display.DisplayControls;
import io.github.mmilk23.screenduo.display.StatusScreenRenderer;
import io.github.mmilk23.screenduo.location.GeoPoint;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class AircraftBrowserController {

    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);
    private static final Duration BUTTON_DEBOUNCE = Duration.ofMillis(600);

    private final Display display;
    private final DisplayControls controls;
    private final AircraftProvider provider;
    private final FlightRouteProvider routeProvider;
    private final AirlineLogoProvider logoProvider;
    private final Map<String, Optional<FlightRoute>> routes = new HashMap<>();
    private final GeoPoint location;
    private final double radiusKm;
    private final Clock clock;
    private final AircraftListScreenRenderer listRenderer = new AircraftListScreenRenderer();
    private final AircraftDetailScreenRenderer detailRenderer = new AircraftDetailScreenRenderer();
    private List<NearbyAircraft> aircraft = List.of();
    private Instant loadedAt = Instant.EPOCH;
    private int selectedIndex;
    private View view = View.LIST;
    private DisplayButton lastButton;
    private Instant lastButtonAt = Instant.MIN;

    public AircraftBrowserController(
            Display display, DisplayControls controls, AircraftProvider provider,
            GeoPoint location, double radiusKm) {
        this(display, controls, provider, location, radiusKm, Clock.systemUTC());
    }

    AircraftBrowserController(
            Display display, DisplayControls controls, AircraftProvider provider,
            GeoPoint location, double radiusKm, Clock clock) {
        this(display, controls, provider, (callsign, time) -> Optional.empty(),
                AirlineLogoProvider.NONE, location, radiusKm, clock);
    }

    public AircraftBrowserController(
            Display display, DisplayControls controls, AircraftProvider provider,
            FlightRouteProvider routeProvider, GeoPoint location, double radiusKm) {
        this(display, controls, provider, routeProvider, AirlineLogoProvider.NONE,
                location, radiusKm, Clock.systemUTC());
    }

    AircraftBrowserController(
            Display display, DisplayControls controls, AircraftProvider provider,
            FlightRouteProvider routeProvider, GeoPoint location, double radiusKm, Clock clock) {
        this(display, controls, provider, routeProvider, AirlineLogoProvider.NONE,
                location, radiusKm, clock);
    }

    public AircraftBrowserController(
            Display display, DisplayControls controls, AircraftProvider provider,
            FlightRouteProvider routeProvider, AirlineLogoProvider logoProvider,
            GeoPoint location, double radiusKm) {
        this(display, controls, provider, routeProvider, logoProvider,
                location, radiusKm, Clock.systemUTC());
    }

    AircraftBrowserController(
            Display display, DisplayControls controls, AircraftProvider provider,
            FlightRouteProvider routeProvider, AirlineLogoProvider logoProvider,
            GeoPoint location, double radiusKm, Clock clock) {
        this.routeProvider = routeProvider;
        this.logoProvider = logoProvider;
        this.display = display;
        this.controls = controls;
        this.provider = provider;
        this.location = location;
        this.radiusKm = radiusKm;
        this.clock = clock;
    }

    public void run() throws InterruptedException {
        loadAircraft();
        System.out.println("Aircraft browser active. Use UP/DOWN, CONFIRM and BACK (Ctrl+C to stop).");
        boolean running = true;
        while (running && !Thread.currentThread().isInterrupted()) {
            Optional<DisplayButtonEvent> event = controls.pollButton();
            if (event.isPresent()) {
                DisplayButton button = event.orElseThrow().button();
                if (button == DisplayButton.UNKNOWN) {
                    lastButton = null;
                } else if (accept(button)) {
                    running = handle(button);
                }
            }
            if (running) {
                Thread.sleep(POLL_INTERVAL.toMillis());
            }
        }
        System.out.println("Aircraft browser finished.");
    }

    private void loadAircraft() throws InterruptedException {
        display.show(new StatusScreenRenderer().render(
                display.geometry(), "AIRCRAFT", "LOADING AIRCRAFT"));
        try {
            aircraft = provider.findNearby(location, radiusKm).stream()
                    .sorted(Comparator.comparingDouble(NearbyAircraft::distanceKm)).toList();
            loadedAt = clock.instant();
            routes.clear();
            selectedIndex = 0;
            view = View.LIST;
            showList();
        } catch (IOException exception) {
            view = View.ERROR;
            display.show(listRenderer.renderError(display.geometry()));
            System.err.println("Unable to load nearby aircraft: " + exception.getMessage());
        }
    }

    private boolean handle(DisplayButton button) throws InterruptedException {
        if (view == View.DETAIL) {
            if (button == DisplayButton.BACK) {
                view = View.LIST;
                showList();
            }
            return true;
        }
        if (button == DisplayButton.BACK) {
            return false;
        }
        if (view == View.ERROR) {
            if (button == DisplayButton.CONFIRM) {
                loadAircraft();
            }
            return true;
        }
        if (aircraft.isEmpty()) {
            return true;
        }
        switch (button) {
            case UP, DOWN -> {
                selectedIndex = Math.floorMod(
                        selectedIndex + (button == DisplayButton.UP ? -1 : 1), aircraft.size());
                showList();
            }
            case CONFIRM -> {
                showSelectedAircraft();
            }
            default -> { }
        }
        return true;
    }

    private void showSelectedAircraft() throws InterruptedException {
        NearbyAircraft selected = aircraft.get(selectedIndex);
        String callsign = selected.callsign().trim().toUpperCase(Locale.ROOT);
        if (!routes.containsKey(callsign)) {
            display.show(new StatusScreenRenderer().render(
                    display.geometry(), "AIRCRAFT", "LOADING ROUTE"));
            try {
                routes.put(callsign, routeProvider.findRoute(callsign, loadedAt));
            } catch (IOException exception) {
                routes.put(callsign, Optional.empty());
                System.err.println("Unable to load planned flight route: " + exception.getMessage());
            }
        }
        view = View.DETAIL;
        display.show(detailRenderer.render(display.geometry(),
                new AircraftDetailScreenData(selected, loadedAt, routes.get(callsign),
                        logoProvider.findLogo(selected.airlineIcaoCode()))));
    }

    private boolean accept(DisplayButton button) {
        Instant now = clock.instant();
        if (button == lastButton && now.isBefore(lastButtonAt.plus(BUTTON_DEBOUNCE))) {
            return false;
        }
        lastButton = button;
        lastButtonAt = now;
        return true;
    }

    private AircraftListScreenData screenData() {
        Map<String, io.github.mmilk23.screenduo.display.RgbFrame> logos = new HashMap<>();
        int pageStart = selectedIndex / AircraftListScreenRenderer.PAGE_SIZE * AircraftListScreenRenderer.PAGE_SIZE;
        int pageEnd = Math.min(pageStart + AircraftListScreenRenderer.PAGE_SIZE, aircraft.size());
        for (int index = pageStart; index < pageEnd; index++) {
            NearbyAircraft item = aircraft.get(index);
            logoProvider.findLogo(item.airlineIcaoCode()).ifPresent(logo -> logos.put(item.airlineIcaoCode(), logo));
        }
        return new AircraftListScreenData(aircraft, selectedIndex, loadedAt, logos);
    }

    private void showList() {
        display.show(listRenderer.render(display.geometry(), screenData()));
    }

    private enum View {
        LIST,
        DETAIL,
        ERROR
    }
}