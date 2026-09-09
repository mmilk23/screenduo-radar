package io.github.mmilk23.screenduo.flight;

import io.github.mmilk23.screenduo.airport.AirportCity;
import io.github.mmilk23.screenduo.airport.AirportCityProvider;
import java.io.IOException;

public final class RouteCityEnricher {

    private final AirportCityProvider cityProvider;

    public RouteCityEnricher(AirportCityProvider cityProvider) {
        this.cityProvider = cityProvider;
    }

    public FlightRoute enrich(FlightRoute route) throws IOException, InterruptedException {
        AirportCity origin = cityProvider.findAirportCity(route.originIcao())
                .orElse(new AirportCity("", ""));
        AirportCity destination = cityProvider.findAirportCity(route.destinationIcao())
                .orElse(new AirportCity("", ""));
        if (origin.city().equals(route.originCity())
                && destination.city().equals(route.destinationCity())
                && origin.displayName().equals(route.originDisplayName())
                && destination.displayName().equals(route.destinationDisplayName())) {
            return route;
        }
        return new FlightRoute(
                route.originIcao(),
                route.destinationIcao(),
                route.scheduledDeparture(),
                route.scheduledArrival(),
                origin.city(),
                destination.city(),
                origin.displayName(),
                destination.displayName());
    }

    public ScheduledFlight enrich(ScheduledFlight flight) throws IOException, InterruptedException {
        return new ScheduledFlight(flight.callsign(), enrich(flight.route()));
    }
}
