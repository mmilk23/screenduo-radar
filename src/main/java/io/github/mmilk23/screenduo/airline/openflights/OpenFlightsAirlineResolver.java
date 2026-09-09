package io.github.mmilk23.screenduo.airline.openflights;

import io.github.mmilk23.screenduo.airline.AirlineResolver;
import io.github.mmilk23.screenduo.data.CachedHttpFile;
import io.github.mmilk23.screenduo.data.CsvReader;
import io.github.mmilk23.screenduo.data.TextDataSource;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class OpenFlightsAirlineResolver implements AirlineResolver {

    private static final URI DATA_URI = URI.create(
            "https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat");

    private final TextDataSource dataSource;
    private volatile Map<String, String> airlinesByIcaoCode;

    public OpenFlightsAirlineResolver() {
        this(new CachedHttpFile(
                DATA_URI,
                Path.of("data", "cache", "airlines.dat"),
                Duration.ofDays(30)));
    }

    OpenFlightsAirlineResolver(TextDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<String> resolve(String callsign) throws IOException, InterruptedException {
        String icaoCode = extractIcaoCode(callsign);
        if (icaoCode == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(loadAirlines().get(icaoCode));
    }

    private Map<String, String> loadAirlines() throws IOException, InterruptedException {
        Map<String, String> loaded = airlinesByIcaoCode;
        if (loaded != null) {
            return loaded;
        }

        synchronized (this) {
            if (airlinesByIcaoCode == null) {
                Map<String, String> airlines = new HashMap<>();
                CsvReader.read(dataSource.get(), row -> {
                    if (row.size() >= 8
                            && !"\\N".equals(row.get(1))
                            && !"\\N".equals(row.get(4))
                            && "Y".equalsIgnoreCase(row.get(7))) {
                        airlines.putIfAbsent(
                                row.get(4).toUpperCase(Locale.ROOT),
                                row.get(1));
                    }
                });
                airlinesByIcaoCode = Map.copyOf(airlines);
            }
            return airlinesByIcaoCode;
        }
    }

    private static String extractIcaoCode(String callsign) {
        if (callsign == null) {
            return null;
        }
        String normalized = callsign.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() < 3
                || !Character.isLetter(normalized.charAt(0))
                || !Character.isLetter(normalized.charAt(1))
                || !Character.isLetter(normalized.charAt(2))) {
            return null;
        }
        return normalized.substring(0, 3);
    }
}
