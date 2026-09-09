package io.github.mmilk23.screenduo.flight;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@FunctionalInterface
public interface AirportFlightProvider {
    /** Flights whose scheduled arrival or departure at this airport falls on the given UTC date. */
    List<ScheduledFlight> findFlights(String airportIcao, LocalDate utcDate)
            throws IOException, InterruptedException;
}