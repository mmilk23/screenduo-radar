package io.github.mmilk23.screenduo.airport.display;

import io.github.mmilk23.screenduo.airport.NearbyAirport;
import java.util.List;
import java.util.Objects;

public record AirportListScreenData(List<NearbyAirport> airports, int selectedIndex) {

    public AirportListScreenData {
        airports = List.copyOf(Objects.requireNonNull(airports, "airports"));
        if (airports.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex < 0 || selectedIndex >= airports.size()) {
            throw new IllegalArgumentException("Selected airport index is outside the list.");
        }
    }
}
