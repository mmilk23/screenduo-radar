package io.github.mmilk23.screenduo.aircraft;

import io.github.mmilk23.screenduo.location.GeoPoint;
import java.io.IOException;
import java.util.List;

public interface AircraftProvider {

    List<NearbyAircraft> findNearby(GeoPoint center, double radiusKm)
            throws IOException, InterruptedException;
}
