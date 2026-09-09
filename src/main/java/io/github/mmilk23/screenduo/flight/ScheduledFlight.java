package io.github.mmilk23.screenduo.flight;

import java.util.Objects;

public record ScheduledFlight(String callsign, FlightRoute route) {
    public ScheduledFlight {
        Objects.requireNonNull(callsign, "callsign");
        Objects.requireNonNull(route, "route");
    }
}