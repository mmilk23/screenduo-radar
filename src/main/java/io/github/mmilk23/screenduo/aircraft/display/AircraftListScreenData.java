package io.github.mmilk23.screenduo.aircraft.display;

import io.github.mmilk23.screenduo.aircraft.NearbyAircraft;
import io.github.mmilk23.screenduo.display.RgbFrame;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record AircraftListScreenData(
        List<NearbyAircraft> aircraft, int selectedIndex, Instant loadedAt, Map<String, RgbFrame> airlineLogos) {

    public AircraftListScreenData(List<NearbyAircraft> aircraft, int selectedIndex, Instant loadedAt) {
        this(aircraft, selectedIndex, loadedAt, Map.of());
    }

    public AircraftListScreenData {
        aircraft = List.copyOf(aircraft);
        airlineLogos = Map.copyOf(airlineLogos);
        Objects.requireNonNull(loadedAt, "loadedAt");
        if (aircraft.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex < 0 || selectedIndex >= aircraft.size()) {
            throw new IllegalArgumentException("Selected aircraft index is outside the list.");
        }
    }
}