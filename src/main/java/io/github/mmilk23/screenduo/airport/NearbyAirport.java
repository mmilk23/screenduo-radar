package io.github.mmilk23.screenduo.airport;

import io.github.mmilk23.screenduo.location.GeoPoint;

public record NearbyAirport(
        String ident,
        String iataCode,
        String name,
        String municipality,
        String countryCode,
        String type,
        GeoPoint position,
        double distanceKm,
        double bearingDegrees,
        Double elevationMeters) {
}
