package io.github.mmilk23.screenduo.config;

import io.github.mmilk23.screenduo.location.GeoPoint;
import java.io.IOException;
import java.nio.file.Path;

public record ApplicationConfig(
        GeoPoint location,
        String city,
        double aircraftRadiusKm,
        double airportRadiusKm) {

    public static final Path DEFAULT_PATH = Path.of("config.ini");
    private static final String LATITUDE = "location.latitude";
    private static final String LONGITUDE = "location.longitude";
    private static final String CITY = "location.city";
    private static final String AIRCRAFT_RADIUS = "location.aircraft_radius_km";
    private static final String AIRPORT_RADIUS = "location.airport_radius_km";
    private static final double DEFAULT_AIRCRAFT_RADIUS_KM = 50.0;
    private static final double DEFAULT_AIRPORT_RADIUS_KM = 100.0;

    public static ApplicationConfig fromDefaultFile() throws IOException {
        return fromFile(DEFAULT_PATH);
    }

    public static ApplicationConfig fromFile(Path path) throws IOException {
        IniConfig config = IniConfig.load(path);
        double latitude = parseDouble(LATITUDE, config.required(LATITUDE));
        double longitude = parseDouble(LONGITUDE, config.required(LONGITUDE));
        String city = config.optional(CITY, "LOCATION").trim();
        double aircraftRadius = parseDouble(
                AIRCRAFT_RADIUS,
                config.optional(AIRCRAFT_RADIUS, Double.toString(DEFAULT_AIRCRAFT_RADIUS_KM)));
        double airportRadius = parseDouble(
                AIRPORT_RADIUS,
                config.optional(AIRPORT_RADIUS, Double.toString(DEFAULT_AIRPORT_RADIUS_KM)));

        validateRadius(AIRCRAFT_RADIUS, aircraftRadius);
        validateRadius(AIRPORT_RADIUS, airportRadius);
        return new ApplicationConfig(
                new GeoPoint(latitude, longitude),
                city.isEmpty() ? "LOCATION" : city,
                aircraftRadius,
                airportRadius);
    }

    private static void validateRadius(String name, double radius) {
        if (!Double.isFinite(radius) || radius <= 0.0 || radius > 1_000.0) {
            throw new IllegalArgumentException(
                    name + " must be greater than 0 and at most 1000.");
        }
    }

    private static double parseDouble(String name, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a decimal number.", exception);
        }
    }
}
