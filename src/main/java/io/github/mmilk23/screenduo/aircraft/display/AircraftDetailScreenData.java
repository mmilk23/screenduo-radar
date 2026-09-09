package io.github.mmilk23.screenduo.aircraft.display;

import io.github.mmilk23.screenduo.aircraft.NearbyAircraft;
import io.github.mmilk23.screenduo.display.RgbFrame;
import io.github.mmilk23.screenduo.flight.FlightRoute;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public record AircraftDetailScreenData(
        NearbyAircraft aircraft, Instant loadedAt, Optional<FlightRoute> route, Optional<RgbFrame> airlineLogo) {

    public AircraftDetailScreenData(NearbyAircraft aircraft, Instant loadedAt, Optional<FlightRoute> route) {
        this(aircraft, loadedAt, route, Optional.empty());
    }

    public AircraftDetailScreenData {
        Objects.requireNonNull(aircraft, "aircraft");
        Objects.requireNonNull(loadedAt, "loadedAt");
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(airlineLogo, "airlineLogo");
    }
}