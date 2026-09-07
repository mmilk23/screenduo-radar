package io.github.mmilk23.screenduo.config;

import io.github.mmilk23.screenduo.location.GeoPoint;
import java.io.IOException;
import java.nio.file.Path;

public record ApplicationConfig(GeoPoint location, double aircraftRadiusKm) {

    public static final Path DEFAULT_PATH = Path.of("config.ini");
    private static final String LATITUDE = "location.latitude";
    private static final String LONGITUDE = "location.longitude";
    private static final String RADIUS = "location.aircraft_radius_km";
    private static final double DEFAULT_RADIUS_KM = 50.0;

    public static ApplicationConfig fromDefaultFile() throws IOException {
        return fromFile(DEFAULT_PATH);
    }

    public static ApplicationConfig fromFile(Path path) throws IOException {
        IniConfig config = IniConfig.load(path);
        double latitude = parseDouble(LATITUDE, config.required(LATITUDE));
        double longitude = parseDouble(LONGITUDE, config.required(LONGITUDE));
        double radius = parseDouble(
                RADIUS, config.optional(RADIUS, Double.toString(DEFAULT_RADIUS_KM)));

        if (!Double.isFinite(radius) || radius <= 0.0 || radius > 1_000.0) {
            throw new IllegalArgumentException(
                    RADIUS + " must be greater than 0 and at most 1000.");
        }
        return new ApplicationConfig(new GeoPoint(latitude, longitude), radius);
    }

    private static double parseDouble(String name, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a decimal number.", exception);
        }
    }
}
