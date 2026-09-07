package io.github.mmilk23.screenduo.config;

import io.github.mmilk23.screenduo.location.GeoPoint;
import java.util.Map;

public record ApplicationConfig(GeoPoint location, double aircraftRadiusKm) {

    private static final String LATITUDE = "SCREENDUO_LATITUDE";
    private static final String LONGITUDE = "SCREENDUO_LONGITUDE";
    private static final String RADIUS = "SCREENDUO_RADIUS_KM";
    private static final double DEFAULT_RADIUS_KM = 50.0;

    public static ApplicationConfig fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static ApplicationConfig fromEnvironment(Map<String, String> environment) {
        double latitude = requiredDouble(environment, LATITUDE);
        double longitude = requiredDouble(environment, LONGITUDE);
        double radius = optionalDouble(environment, RADIUS, DEFAULT_RADIUS_KM);
        if (!Double.isFinite(radius) || radius <= 0.0 || radius > 1_000.0) {
            throw new IllegalArgumentException(RADIUS + " must be greater than 0 and at most 1000.");
        }
        return new ApplicationConfig(new GeoPoint(latitude, longitude), radius);
    }

    private static double requiredDouble(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Environment variable " + name + " is required.");
        }
        return parseDouble(name, value);
    }

    private static double optionalDouble(
            Map<String, String> environment, String name, double defaultValue) {
        String value = environment.get(name);
        return value == null || value.isBlank() ? defaultValue : parseDouble(name, value);
    }

    private static double parseDouble(String name, String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a decimal number.", exception);
        }
    }
}
