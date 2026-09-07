package io.github.mmilk23.screenduo.airport;

import io.github.mmilk23.screenduo.location.GeoPoint;
import java.io.IOException;
import java.util.List;

public interface AirportProvider {

    List<NearbyAirport> findNearby(GeoPoint center, double radiusKm)
            throws IOException, InterruptedException;
}
