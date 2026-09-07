package io.github.mmilk23.screenduo;

import io.github.mmilk23.screenduo.aircraft.AircraftProvider;
import io.github.mmilk23.screenduo.aircraft.NearbyAircraft;
import io.github.mmilk23.screenduo.aircraft.opensky.OpenSkyAircraftProvider;
import io.github.mmilk23.screenduo.airport.AirportProvider;
import io.github.mmilk23.screenduo.airport.NearbyAirport;
import io.github.mmilk23.screenduo.airport.ourairports.OurAirportsAirportProvider;
import io.github.mmilk23.screenduo.config.ApplicationConfig;
import io.github.mmilk23.screenduo.device.ScreenDuoDevice;
import io.github.mmilk23.screenduo.display.ClassicTvTestPattern;
import io.github.mmilk23.screenduo.display.Display;
import io.github.mmilk23.screenduo.display.DisplayButtonEvent;
import io.github.mmilk23.screenduo.display.DisplayControls;
import io.github.mmilk23.screenduo.weather.WeatherConditions;
import io.github.mmilk23.screenduo.weather.WeatherProvider;
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

    private static final String API_TEST_ARGUMENT = "--api-test";
    private static final String TEST_PATTERN_ARGUMENT = "--test-pattern";
    private static final String BUTTON_TEST_ARGUMENT = "--button-test";
    private static final Duration BUTTON_TEST_DURATION = Duration.ofSeconds(30);

    private ScreenDuoApplication() {
    }

    public static void main(String[] args) {
        try {
            if (hasArgument(args, API_TEST_ARGUMENT)) {
                testApis();
                return;
            }

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

                boolean testPatternRequested = hasArgument(args, TEST_PATTERN_ARGUMENT);
                boolean buttonTestRequested = hasArgument(args, BUTTON_TEST_ARGUMENT);

                if (testPatternRequested || buttonTestRequested) {
                    showTestPattern(device);
                }
                if (buttonTestRequested) {
                    testButtons(device);
                }
                if (!testPatternRequested && !buttonTestRequested) {
                    System.out.println(
                            "Diagnostic mode only. Use --test-pattern, --button-test or --api-test.");
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
        }
    }

    private static void testApis() throws IOException, InterruptedException {
        ApplicationConfig config = ApplicationConfig.fromDefaultFile();
        WeatherProvider weatherProvider = new OpenMeteoWeatherProvider();
        AircraftProvider aircraftProvider = new OpenSkyAircraftProvider();
        AirportProvider airportProvider = new OurAirportsAirportProvider();

        System.out.printf(Locale.ROOT,
                "Location: %.6f, %.6f | aircraft radius: %.1f km | airport radius: %.1f km%n",
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
        System.out.printf(Locale.ROOT, "Airports found inside radius: %d%n", airports.size());
        airports.stream().limit(15).forEach(ScreenDuoApplication::printAirport);
        if (airports.size() > 15) {
            System.out.printf(Locale.ROOT, "... and %d more.%n", airports.size() - 15);
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
                        + " | altitude %s m | speed %s km/h | %s%n",
                name,
                airline,
                aircraft.distanceKm(),
                aircraft.bearingDegrees(),
                value(aircraft.altitudeMeters(), "%.0f"),
                value(aircraft.speedKilometersPerHour(), "%.0f"),
                aircraft.onGround() ? "on ground" : aircraft.originCountry());
    }

    private static void printAirport(NearbyAirport airport) {
        String code = airport.iataCode().isBlank() ? airport.ident() : airport.iataCode();
        String municipality = airport.municipality().isBlank()
                ? airport.countryCode() : airport.municipality();
        System.out.printf(Locale.ROOT,
                "  %-5s | %-35s | %-20s | %6.1f km | bearing %03.0f | %s%n",
                code,
                airport.name(),
                municipality,
                airport.distanceKm(),
                airport.bearingDegrees(),
                airport.type());
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
