package io.github.mmilk23.screenduo.airport.ourairports;

import io.github.mmilk23.screenduo.airport.AirportProvider;
import io.github.mmilk23.screenduo.airport.NearbyAirport;
import io.github.mmilk23.screenduo.data.CachedHttpFile;
import io.github.mmilk23.screenduo.data.CsvReader;
import io.github.mmilk23.screenduo.data.TextDataSource;
import io.github.mmilk23.screenduo.location.GeoMath;
import io.github.mmilk23.screenduo.location.GeoPoint;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class OurAirportsAirportProvider implements AirportProvider {

    private static final URI DATA_URI = URI.create(
            "https://davidmegginson.github.io/ourairports-data/airports.csv");
    private static final double FEET_TO_METERS = 0.3048;

    private final TextDataSource dataSource;

    public OurAirportsAirportProvider() {
        this(new CachedHttpFile(
                DATA_URI,
                Path.of("data", "cache", "airports.csv"),
                Duration.ofDays(7)));
    }

    OurAirportsAirportProvider(TextDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public List<NearbyAirport> findNearby(GeoPoint center, double radiusKm)
            throws IOException, InterruptedException {
        List<NearbyAirport> airports = new ArrayList<>();
        CsvReader.read(dataSource.get(), row -> addIfNearby(row, center, radiusKm, airports));
        airports.sort(Comparator.comparingDouble(NearbyAirport::distanceKm));
        return List.copyOf(airports);
    }

    private static void addIfNearby(
            List<String> row,
            GeoPoint center,
            double radiusKm,
            List<NearbyAirport> airports) {
        if (row.size() < 16 || "id".equals(row.get(0)) || "closed".equals(row.get(2))) {
            return;
        }

        Double latitude = decimal(row.get(4));
        Double longitude = decimal(row.get(5));
        if (latitude == null || longitude == null) {
            return;
        }

        GeoPoint position;
        try {
            position = new GeoPoint(latitude, longitude);
        } catch (IllegalArgumentException exception) {
            return;
        }

        double distance = GeoMath.distanceKm(center, position);
        if (distance > radiusKm) {
            return;
        }

        Double elevationFeet = decimal(row.get(6));
        airports.add(new NearbyAirport(
                row.get(1),
                row.get(13),
                row.get(3),
                row.get(10),
                row.get(8),
                row.get(2),
                position,
                distance,
                GeoMath.bearingDegrees(center, position),
                elevationFeet == null ? null : elevationFeet * FEET_TO_METERS));
    }

    private static Double decimal(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
