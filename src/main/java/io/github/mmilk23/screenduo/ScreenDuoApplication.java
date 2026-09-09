package io.github.mmilk23.screenduo;

import io.github.mmilk23.screenduo.aircraft.AircraftBrowserController;
import io.github.mmilk23.screenduo.aircraft.AircraftProvider;
import io.github.mmilk23.screenduo.aircraft.NearbyAircraft;
import io.github.mmilk23.screenduo.aircraft.opensky.OpenSkyAircraftProvider;
import io.github.mmilk23.screenduo.airline.logo.CachedAirlineLogoProvider;
import io.github.mmilk23.screenduo.flight.FlightRoute;
import io.github.mmilk23.screenduo.flight.RouteCityEnricher;
import io.github.mmilk23.screenduo.flight.ScheduledFlight;
import io.github.mmilk23.screenduo.flight.siros.SirosFlightRouteProvider;
import io.github.mmilk23.screenduo.airport.AirportBrowserController;
import io.github.mmilk23.screenduo.airport.AirportProvider;
import io.github.mmilk23.screenduo.airport.NearbyAirport;
import io.github.mmilk23.screenduo.airport.ourairports.OurAirportsAirportProvider;
import io.github.mmilk23.screenduo.config.ApplicationConfig;
import io.github.mmilk23.screenduo.navigation.RadarDashboardController;
import io.github.mmilk23.screenduo.device.ScreenDuoDevice;
import io.github.mmilk23.screenduo.display.ClassicTvTestPattern;
import io.github.mmilk23.screenduo.display.Display;
import io.github.mmilk23.screenduo.display.DisplayButtonEvent;
import io.github.mmilk23.screenduo.display.DisplayControls;
import io.github.mmilk23.screenduo.weather.WeatherConditions;
import io.github.mmilk23.screenduo.weather.WeatherProvider;
import io.github.mmilk23.screenduo.weather.display.WeatherScreenData;
import io.github.mmilk23.screenduo.weather.display.WeatherScreenRenderer;
import io.github.mmilk23.screenduo.weather.openmeteo.OpenMeteoWeatherProvider;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.usb4java.LibUsbException;

public final class ScreenDuoApplication {

    private static final String DASHBOARD_ARGUMENT = "--dashboard";
    private static final String API_TEST_ARGUMENT = "--api-test";
    private static final String WEATHER_SCREEN_ARGUMENT = "--weather-screen";
    private static final String AIRPORT_SCREEN_ARGUMENT = "--airport-screen";
    private static final String AIRCRAFT_SCREEN_ARGUMENT = "--aircraft-screen";
    private static final String TEST_PATTERN_ARGUMENT = "--test-pattern";
    private static final String BUTTON_TEST_ARGUMENT = "--button-test";
    private static final String TRACE_NAVIGATION_ARGUMENT = "--trace-navigation";
    private static final Duration BUTTON_TEST_DURATION = Duration.ofSeconds(30);
    private static final Duration STARTUP_TEST_PATTERN_DURATION = Duration.ofMillis(900);

    private ScreenDuoApplication() {
    }

    public static void main(String[] args) {
        try {
            if (hasArgument(args, API_TEST_ARGUMENT)) {
                testApis();
                return;
            }

            boolean startupDashboardRequested = args.length == 0
                    || (args.length == 1 && hasArgument(args, TRACE_NAVIGATION_ARGUMENT));
            boolean dashboardRequested = startupDashboardRequested || hasArgument(args, DASHBOARD_ARGUMENT);
            ApplicationConfig dashboardConfig =
                    dashboardRequested ? ApplicationConfig.fromDefaultFile() : null;
            boolean aircraftScreenRequested =
                    !dashboardRequested && hasArgument(args, AIRCRAFT_SCREEN_ARGUMENT);
            ApplicationConfig aircraftConfig =
                    aircraftScreenRequested ? ApplicationConfig.fromDefaultFile() : null;
            boolean airportScreenRequested =
                    !dashboardRequested && !aircraftScreenRequested && hasArgument(args, AIRPORT_SCREEN_ARGUMENT);
            List<NearbyAirport> browserAirports =
                    airportScreenRequested ? loadNearbyAirports() : List.of();
            boolean weatherScreenRequested =
                    !dashboardRequested && !aircraftScreenRequested && !airportScreenRequested
                            && hasArgument(args, WEATHER_SCREEN_ARGUMENT);
            WeatherScreenData weatherScreen = weatherScreenRequested ? loadWeatherScreen() : null;

            Optional<ScreenDuoDevice> detectedDevice = ScreenDuoDevice.open();
            if (detectedDevice.isEmpty()) {
                System.out.println("ScreenDUO not found (expected USB 1043:3100).");
                return;
            }

            try (ScreenDuoDevice device = detectedDevice.orElseThrow()) {
                System.out.printf(
                        Locale.ROOT,
                        "ScreenDUO detected and opened successfully (%04x:%04x).%n",
                        Short.toUnsignedInt(ScreenDuoDevice.VENDOR_ID),
                        Short.toUnsignedInt(ScreenDuoDevice.PRODUCT_ID));
                System.out.print(device.descriptorReport());

                if (dashboardRequested) {
                    if (startupDashboardRequested) {
                        showTestPattern(device);
                        Thread.sleep(STARTUP_TEST_PATTERN_DURATION.toMillis());
                    }
                    SirosFlightRouteProvider siros = new SirosFlightRouteProvider();
                    OurAirportsAirportProvider airports = new OurAirportsAirportProvider();
                    RouteCityEnricher routeCities = new RouteCityEnricher(airports);
                    new RadarDashboardController(device, device, dashboardConfig,
                            new OpenMeteoWeatherProvider(), new OpenSkyAircraftProvider(),
                            airports,
                            (callsign, reference) -> enrichRoute(siros.findRoute(callsign, reference), routeCities),
                            (airport, date) -> enrichFlights(siros.findFlights(airport, date), routeCities),
                            new CachedAirlineLogoProvider())
                            .run(hasArgument(args, TRACE_NAVIGATION_ARGUMENT));
                    return;
                }

                if (aircraftScreenRequested) {
                    SirosFlightRouteProvider siros = new SirosFlightRouteProvider();
                    RouteCityEnricher routeCities = new RouteCityEnricher(new OurAirportsAirportProvider());
                    new AircraftBrowserController(
                            device, device, new OpenSkyAircraftProvider(),
                            (callsign, reference) -> enrichRoute(siros.findRoute(callsign, reference), routeCities),
                            new CachedAirlineLogoProvider(),
                            aircraftConfig.location(), aircraftConfig.aircraftRadiusKm()).run();
                    return;
                }

                if (airportScreenRequested) {
                    new AirportBrowserController(
                            device,
                            device,
                            browserAirports,
                            new OpenMeteoWeatherProvider()).run();
                    return;
                }

                boolean testPatternRequested = hasArgument(args, TEST_PATTERN_ARGUMENT);
                boolean buttonTestRequested = hasArgument(args, BUTTON_TEST_ARGUMENT);

                if (testPatternRequested || (buttonTestRequested && !weatherScreenRequested)) {
                    showTestPattern(device);
                }
                if (weatherScreenRequested) {
                    showWeather(device, weatherScreen);
                }
                if (buttonTestRequested) {
                    testButtons(device);
                }
                if (!testPatternRequested && !buttonTestRequested && !weatherScreenRequested) {
                    System.out.println("Diagnostic mode only. Use --test-pattern, --button-test, "
                            + "--dashboard, --weather-screen, --airport-screen, --aircraft-screen or --api-test.");
                }
            }
        } catch (IOException exception) {
            System.err.println("Unable to read configuration or query remote data: "
                    + exception.getMessage());
            System.exit(1);
        } catch (LibUsbException | IllegalStateException | IllegalArgumentException exception) {
            System.err.println("Unable to run ScreenDUO Radar: " + exception.getMessage());
            System.exit(1);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Operation interrupted.");
        } catch (Throwable throwable) {
            System.err.println("Unexpected ScreenDUO Radar failure: "
                    + throwable.getClass().getName() + ": " + throwable.getMessage());
            throwable.printStackTrace(System.err);
            System.exit(1);
        }
    }


    private static Optional<FlightRoute> enrichRoute(
            Optional<FlightRoute> route, RouteCityEnricher routeCities)
            throws IOException, InterruptedException {
        return route.isPresent() ? Optional.of(routeCities.enrich(route.orElseThrow())) : Optional.empty();
    }

    private static List<ScheduledFlight> enrichFlights(
            List<ScheduledFlight> flights, RouteCityEnricher routeCities)
            throws IOException, InterruptedException {
        List<ScheduledFlight> enriched = new java.util.ArrayList<>(flights.size());
        for (ScheduledFlight flight : flights) {
            enriched.add(routeCities.enrich(flight));
        }
        return List.copyOf(enriched);
    }
    private static List<NearbyAirport> loadNearbyAirports()
            throws IOException, InterruptedException {
        ApplicationConfig config = ApplicationConfig.fromDefaultFile();
        AirportProvider airportProvider = new OurAirportsAirportProvider();
        List<NearbyAirport> airports =
                airportProvider.findNearby(config.location(), config.airportRadiusKm());
        System.out.printf(Locale.ROOT,
                "Loaded %d relevant airports inside %.1f km.%n",
                airports.size(),
                config.airportRadiusKm());
        return airports;
    }

    private static WeatherScreenData loadWeatherScreen()
            throws IOException, InterruptedException {
        ApplicationConfig config = ApplicationConfig.fromDefaultFile();
        WeatherProvider weatherProvider = new OpenMeteoWeatherProvider();
        WeatherConditions conditions = weatherProvider.currentConditions(config.location());
        return new WeatherScreenData(config.city(), conditions);
    }

    private static void testApis() throws IOException, InterruptedException {
        ApplicationConfig config = ApplicationConfig.fromDefaultFile();
        WeatherProvider weatherProvider = new OpenMeteoWeatherProvider();
        AircraftProvider aircraftProvider = new OpenSkyAircraftProvider();
        AirportProvider airportProvider = new OurAirportsAirportProvider();

        System.out.printf(Locale.ROOT,
                "Location: %s | %.6f, %.6f | aircraft radius: %.1f km | airport radius: %.1f km%n",
                config.city(),
                config.location().latitude(),
                config.location().longitude(),
                config.aircraftRadiusKm(),
                config.airportRadiusKm());

        WeatherConditions weather = weatherProvider.currentConditions(config.location());
        System.out.printf(Locale.ROOT,
                "Weather: %s C (feels like %s C), humidity %s%%, precipitation %s mm%n",
                value(weather.temperatureCelsius(), "%.1f"),
                value(weather.apparentTemperatureCelsius(), "%.1f"),
                value(weather.relativeHumidityPercent(), "%d"),
                value(weather.precipitationMillimeters(), "%.1f"));
        System.out.printf(Locale.ROOT,
                "         clouds %s%%, pressure %s hPa, wind %s km/h at %s degrees, WMO code %s%n",
                value(weather.cloudCoverPercent(), "%d"),
                value(weather.pressureHectopascals(), "%.0f"),
                value(weather.windSpeedKilometersPerHour(), "%.1f"),
                value(weather.windDirectionDegrees(), "%.0f"),
                value(weather.weatherCode(), "%d"));

        List<NearbyAircraft> aircraft =
                aircraftProvider.findNearby(config.location(), config.aircraftRadiusKm());
        List<NearbyAircraft> airborne =
                aircraft.stream().filter(item -> !item.onGround()).toList();
        List<NearbyAircraft> onGround =
                aircraft.stream().filter(NearbyAircraft::onGround).toList();

        printAircraftGroup("Airborne aircraft", airborne, 20);
        printAircraftGroup("Aircraft on ground", onGround, 10);

        List<NearbyAirport> airports =
                airportProvider.findNearby(config.location(), config.airportRadiusKm());
        System.out.printf(Locale.ROOT,
                "Relevant airports found inside radius: %d%n", airports.size());
        airports.stream().limit(5).forEach(ScreenDuoApplication::printAirport);
        if (airports.size() > 5) {
            System.out.printf(Locale.ROOT, "... and %d more.%n", airports.size() - 5);
        }
    }

    private static void printAircraftGroup(
            String title,
            List<NearbyAircraft> aircraft,
            int limit) {
        System.out.printf(Locale.ROOT, "%s: %d%n", title, aircraft.size());
        aircraft.stream().limit(limit).forEach(ScreenDuoApplication::printAircraft);
        if (aircraft.size() > limit) {
            System.out.printf(Locale.ROOT, "... and %d more.%n", aircraft.size() - limit);
        }
    }

    private static void printAircraft(NearbyAircraft aircraft) {
        String name = aircraft.callsign().isBlank() ? aircraft.icao24() : aircraft.callsign();
        String airline = aircraft.airlineName().isBlank()
                ? "unknown operator" : aircraft.airlineName();
        System.out.printf(Locale.ROOT,
                "  %-9s | %-28s | %6.1f km | bearing %03.0f"
                        + " | altitude %s m | speed %s km/h | registration: %s%n",
                name,
                fit(airline, 28),
                aircraft.distanceKm(),
                aircraft.bearingDegrees(),
                value(aircraft.altitudeMeters(), "%.0f"),
                value(aircraft.speedKilometersPerHour(), "%.0f"),
                aircraft.registrationCountry());
    }

    private static void printAirport(NearbyAirport airport) {
        String code = airport.iataCode().isBlank() ? airport.ident() : airport.iataCode();
        String municipality = airport.municipality().isBlank()
                ? airport.countryCode() : airport.municipality();
        System.out.printf(Locale.ROOT,
                "  %-5s | %-35s | %-20s | %6.1f km | bearing %03.0f | %s%n",
                code,
                fit(airport.name(), 35),
                fit(municipality, 20),
                airport.distanceKm(),
                airport.bearingDegrees(),
                airport.type());
    }

    private static String fit(String value, int maximumLength) {
        if (value.length() <= maximumLength) {
            return value;
        }
        return value.substring(0, maximumLength - 3) + "...";
    }

    private static String value(Number number, String format) {
        return number == null ? "n/a" : String.format(Locale.ROOT, format, number);
    }

    private static boolean hasArgument(String[] args, String expected) {
        return Arrays.asList(args).contains(expected);
    }

    private static void showTestPattern(Display display) {
        display.show(ClassicTvTestPattern.render(display.geometry()));
        System.out.println("Classic TV test pattern sent successfully.");
    }

    private static void showWeather(Display display, WeatherScreenData weatherScreen) {
        display.show(new WeatherScreenRenderer().render(display.geometry(), weatherScreen));
        System.out.println("Current weather screen sent successfully.");
    }

    private static void testButtons(DisplayControls controls) throws InterruptedException {
        Instant deadline = Instant.now().plus(BUTTON_TEST_DURATION);
        System.out.println(
                "Button test active for 30 seconds. Press the ScreenDUO buttons (Ctrl+C to stop).");

        while (Instant.now().isBefore(deadline)) {
            Optional<DisplayButtonEvent> event = controls.pollButton();
            event.ifPresent(button -> System.out.printf(
                    Locale.ROOT,
                    "Button: %-8s | ScreenDUO code: %d%n",
                    button.button(),
                    button.sourceCode()));
            Thread.sleep(100);
        }

        System.out.println("Button test finished.");
    }
}
