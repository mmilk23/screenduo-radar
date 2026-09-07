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
import java.util.Set;

public final class OurAirportsAirportProvider implements AirportProvider {

    private static final URI DATA_URI = URI.create(
            "https://davidmegginson.github.io/ourairports-data/airports.csv");
    private static final double FEET_TO_METERS = 0.3048;
    private static final Set<String> PRIMARY_TYPES =
            Set.of("large_airport", "medium_airport");
    private static final String FALLBACK_TYPE = "small_airport";

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
        List<NearbyAirport> primary = new ArrayList<>();
        List<NearbyAirport> fallback = new ArrayList<>();
        CsvReader.read(dataSource.get(), row ->
                addIfNearby(row, center, radiusKm, primary, fallback));

        List<NearbyAirport> selected = primary.isEmpty() ? fallback : primary;
        selected.sort(Comparator.comparingDouble(NearbyAirport::distanceKm));
        return List.copyOf(selected);
    }

    private static void addIfNearby(
            List<String> row,
            GeoPoint center,
            double radiusKm,
            List<NearbyAirport> primary,
            List<NearbyAirport> fallback) {
        if (row.size() < 16 || "id".equals(row.get(0))) {
            return;
        }

        String type = row.get(2);
        if (!PRIMARY_TYPES.contains(type) && !FALLBACK_TYPE.equals(type)) {
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
        NearbyAirport airport = new NearbyAirport(
                row.get(1),
                row.get(13),
                row.get(3),
                row.get(10),
                row.get(8),
                type,
                position,
                distance,
                GeoMath.bearingDegrees(center, position),
                elevationFeet == null ? null : elevationFeet * FEET_TO_METERS);

        if (PRIMARY_TYPES.contains(type)) {
            primary.add(airport);
        } else {
            fallback.add(airport);
        }
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
