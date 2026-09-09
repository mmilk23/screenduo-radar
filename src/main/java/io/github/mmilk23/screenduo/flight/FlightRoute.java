package io.github.mmilk23.screenduo.flight;

import java.time.Instant;
import java.util.Objects;

/** Scheduled route information, not a confirmation of actual aircraft movement. */
public record FlightRoute(
        String originIcao,
        String destinationIcao,
        Instant scheduledDeparture,
        Instant scheduledArrival,
        String originCity,
        String destinationCity,
        String originDisplayName,
        String destinationDisplayName) {

    public FlightRoute(
            String originIcao, String destinationIcao, Instant scheduledDeparture, Instant scheduledArrival) {
        this(originIcao, destinationIcao, scheduledDeparture, scheduledArrival, "", "");
    }

    public FlightRoute(
            String originIcao,
            String destinationIcao,
            Instant scheduledDeparture,
            Instant scheduledArrival,
            String originCity,
            String destinationCity) {
        this(originIcao, destinationIcao, scheduledDeparture, scheduledArrival,
                originCity, destinationCity, originCity, destinationCity);
    }

    public FlightRoute {
        Objects.requireNonNull(originIcao, "originIcao");
        Objects.requireNonNull(destinationIcao, "destinationIcao");
        Objects.requireNonNull(scheduledDeparture, "scheduledDeparture");
        Objects.requireNonNull(scheduledArrival, "scheduledArrival");
        originCity = Objects.requireNonNull(originCity, "originCity").trim();
        destinationCity = Objects.requireNonNull(destinationCity, "destinationCity").trim();
        originDisplayName = Objects.requireNonNull(originDisplayName, "originDisplayName").trim();
        destinationDisplayName = Objects.requireNonNull(destinationDisplayName, "destinationDisplayName").trim();
        if (!originIcao.matches("[A-Z]{4}") || !destinationIcao.matches("[A-Z]{4}")
                || !scheduledArrival.isAfter(scheduledDeparture)) {
            throw new IllegalArgumentException("Invalid scheduled flight route.");
        }
    }

    public String originLabel() {
        return airportLabel(originIcao, originDisplayName());
    }

    public String destinationLabel() {
        return airportLabel(destinationIcao, destinationDisplayName());
    }

    public String originDisplayName() {
        return airportDisplayName(originIcao, originDisplayName);
    }

    public String destinationDisplayName() {
        return airportDisplayName(destinationIcao, destinationDisplayName);
    }

    private static String airportLabel(String icao, String displayName) {
        return displayName.isBlank() ? icao : icao + " (" + displayName + ")";
    }

    private static String airportDisplayName(String icao, String displayName) {
        return displayName.isBlank() ? icao : displayName;
    }
}
