package io.github.mmilk23.screenduo.flight;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;

@FunctionalInterface
public interface FlightRouteProvider {

    /** Returns no route when the flight cannot be identified unambiguously. */
    Optional<FlightRoute> findRoute(String callsign, Instant referenceTime)
            throws IOException, InterruptedException;
}