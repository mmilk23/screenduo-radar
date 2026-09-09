package io.github.mmilk23.screenduo.airport;

import io.github.mmilk23.screenduo.airport.display.AirportListScreenData;
import io.github.mmilk23.screenduo.airport.display.AirportListScreenRenderer;
import io.github.mmilk23.screenduo.display.Display;
import io.github.mmilk23.screenduo.display.DisplayButton;
import io.github.mmilk23.screenduo.display.DisplayButtonEvent;
import io.github.mmilk23.screenduo.display.DisplayControls;
import io.github.mmilk23.screenduo.display.StatusScreenRenderer;
import io.github.mmilk23.screenduo.weather.WeatherConditions;
import io.github.mmilk23.screenduo.weather.WeatherProvider;
import io.github.mmilk23.screenduo.weather.display.WeatherScreenData;
import io.github.mmilk23.screenduo.weather.display.WeatherScreenRenderer;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class AirportBrowserController {

    private static final Duration POLL_INTERVAL = Duration.ofMillis(100);
    private static final Duration BUTTON_DEBOUNCE = Duration.ofMillis(600);

    private final Display display;
    private final DisplayControls controls;
    private final List<NearbyAirport> airports;
    private final WeatherProvider weatherProvider;
    private final AirportListScreenRenderer listRenderer = new AirportListScreenRenderer();
    private final WeatherScreenRenderer weatherRenderer = new WeatherScreenRenderer();
    private final StatusScreenRenderer statusRenderer = new StatusScreenRenderer();

    private View view = View.LIST;
    private int selectedIndex;
    private DisplayButton lastButton;
    private Instant lastButtonAt = Instant.MIN;

    public AirportBrowserController(
            Display display,
            DisplayControls controls,
            List<NearbyAirport> airports,
            WeatherProvider weatherProvider) {
        this.display = display;
        this.controls = controls;
        this.airports = List.copyOf(airports);
        this.weatherProvider = weatherProvider;
    }

    public void run() throws InterruptedException {
        showList();
        System.out.println(
                "Airport browser active. Use UP/DOWN, CONFIRM and BACK (Ctrl+C to stop).");

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
            Thread.sleep(POLL_INTERVAL.toMillis());
        }
        System.out.println("Airport browser finished.");
    }

    private boolean handle(DisplayButton button) {
        if (view == View.LIST) {
            return handleListButton(button);
        }
        if (button == DisplayButton.BACK) {
            view = View.LIST;
            showList();
        }
        return true;
    }

    private boolean handleListButton(DisplayButton button) {
        return switch (button) {
            case UP -> {
                moveSelection(-1);
                yield true;
            }
            case DOWN -> {
                moveSelection(1);
                yield true;
            }
            case CONFIRM -> {
                showSelectedAirportWeather();
                yield true;
            }
            case BACK -> false;
            default -> true;
        };
    }

    private void moveSelection(int delta) {
        if (airports.isEmpty()) {
            return;
        }
        selectedIndex = Math.floorMod(selectedIndex + delta, airports.size());
        showList();
    }

    private void showList() {
        display.show(listRenderer.render(
                display.geometry(),
                new AirportListScreenData(airports, selectedIndex)));
    }

    private void showSelectedAirportWeather() {
        if (airports.isEmpty()) {
            return;
        }

        NearbyAirport airport = airports.get(selectedIndex);
        String code = airport.iataCode().isBlank() ? airport.ident() : airport.iataCode();
        display.show(statusRenderer.render(display.geometry(), code, "LOADING WEATHER"));

        try {
            WeatherConditions weather =
                    weatherProvider.currentConditions(airport.position());
            String title = code + " " + airport.municipality();
            display.show(weatherRenderer.render(
                    display.geometry(),
                    new WeatherScreenData(title, weather)));
            view = View.WEATHER;
            System.out.printf(Locale.ROOT,
                    "Weather loaded for %s - %s.%n", code, airport.name());
        } catch (IOException exception) {
            display.show(statusRenderer.render(display.geometry(), code, "WEATHER ERROR"));
            view = View.WEATHER;
            System.err.println("Unable to load airport weather: " + exception.getMessage());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean accept(DisplayButton button) {
        Instant now = Instant.now();
        if (button == lastButton
                && now.isBefore(lastButtonAt.plus(BUTTON_DEBOUNCE))) {
            return false;
        }
        lastButton = button;
        lastButtonAt = now;
        return true;
    }

    private enum View {
        LIST,
        WEATHER
    }
}
