package io.github.mmilk23.screenduo.aircraft;

import io.github.mmilk23.screenduo.location.GeoPoint;

public record NearbyAircraft(
        String icao24,
        String callsign,
        String airlineName,
        String registrationCountry,
        GeoPoint position,
        double distanceKm,
        double bearingDegrees,
        Double altitudeMeters,
        Double speedKilometersPerHour,
        Double trackDegrees,
        boolean onGround) {
}
