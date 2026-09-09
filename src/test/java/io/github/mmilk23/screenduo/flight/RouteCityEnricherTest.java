package io.github.mmilk23.screenduo.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.mmilk23.screenduo.airport.AirportCity;
import io.github.mmilk23.screenduo.airport.AirportCityProvider;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RouteCityEnricherTest {

    private static final Instant DEPARTURE = Instant.parse("2026-09-09T12:00:00Z");
    private static final Instant ARRIVAL = Instant.parse("2026-09-09T13:00:00Z");

    @Test
    void enrichesCitiesAndDisplayNames() throws Exception {
        AirportCityProvider provider = provider(Map.of(
                "SBGL", new AirportCity("Rio de Janeiro", "Galeao"),
                "SBSP", new AirportCity("Sao Paulo", "Congonhas")));
        RouteCityEnricher enricher = new RouteCityEnricher(provider);
        FlightRoute route = new FlightRoute("SBGL", "SBSP", DEPARTURE, ARRIVAL);

        FlightRoute enriched = enricher.enrich(route);

        assertEquals("Rio de Janeiro", enriched.originCity());
        assertEquals("Sao Paulo", enriched.destinationCity());
        assertEquals("Galeao", enriched.originDisplayName());
        assertEquals("Congonhas", enriched.destinationDisplayName());
    }

    @Test
    void returnsSameRouteWhenNothingChanges() throws Exception {
        AirportCityProvider provider = provider(Map.of(
                "SBGL", new AirportCity("Rio", "Galeao"),
                "SBSP", new AirportCity("Sao Paulo", "Congonhas")));
        RouteCityEnricher enricher = new RouteCityEnricher(provider);
        FlightRoute route = new FlightRoute(
                "SBGL", "SBSP", DEPARTURE, ARRIVAL,
                "Rio", "Sao Paulo", "Galeao", "Congonhas");

        assertSame(route, enricher.enrich(route));
    }

    @Test
    void usesEmptyAirportDataWhenProviderHasNoMatch() throws Exception {
        RouteCityEnricher enricher = new RouteCityEnricher(provider(Map.of()));
        FlightRoute route = new FlightRoute(
                "SBGL", "SBSP", DEPARTURE, ARRIVAL,
                "Old Origin", "Old Destination", "Old Airport", "Old Airport 2");

        FlightRoute enriched = enricher.enrich(route);

        assertEquals("", enriched.originCity());
        assertEquals("", enriched.destinationCity());
        assertEquals("SBGL", enriched.originDisplayName());
        assertEquals("SBSP", enriched.destinationDisplayName());
    }

    @Test
    void enrichesScheduledFlightAndPreservesCallsign() throws Exception {
        RouteCityEnricher enricher = new RouteCityEnricher(provider(Map.of(
                "SBGL", new AirportCity("Rio", "Galeao"),
                "SBSP", new AirportCity("Sao Paulo", "Congonhas"))));
        ScheduledFlight flight = new ScheduledFlight(
                "GLO1234", new FlightRoute("SBGL", "SBSP", DEPARTURE, ARRIVAL));

        ScheduledFlight enriched = enricher.enrich(flight);

        assertEquals("GLO1234", enriched.callsign());
        assertEquals("Rio", enriched.route().originCity());
    }

    @Test
    void propagatesProviderFailures() {
        AirportCityProvider provider = airportIcao -> {
            throw new IOException("catalog unavailable");
        };
        RouteCityEnricher enricher = new RouteCityEnricher(provider);
        FlightRoute route = new FlightRoute("SBGL", "SBSP", DEPARTURE, ARRIVAL);

        assertThrows(IOException.class, () -> enricher.enrich(route));
    }

    private static AirportCityProvider provider(Map<String, AirportCity> airports) {
        return airportIcao -> Optional.ofNullable(airports.get(airportIcao));
    }
}
