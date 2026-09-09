package io.github.mmilk23.screenduo.airline.logo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mmilk23.screenduo.data.CachedHttpFile;
import io.github.mmilk23.screenduo.data.CsvReader;
import io.github.mmilk23.screenduo.data.TextDataSource;
import io.github.mmilk23.screenduo.display.RgbFrame;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class CachedAirlineLogoProvider implements AirlineLogoProvider {

    private static final URI PASSENGER_DATA_URI = URI.create(
            "https://raw.githubusercontent.com/dotmarn/Airlines/master/passenger.json");
    private static final URI OPENFLIGHTS_DATA_URI = URI.create(
            "https://raw.githubusercontent.com/jpatokal/openflights/master/data/airlines.dat");

    private final Path logoDirectory;
    private final TextDataSource passengerData;
    private final TextDataSource openFlightsData;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Function<String, URI> fallbackLogoUri;
    private final Map<String, Optional<RgbFrame>> logos = new HashMap<>();
    private volatile Map<String, String> iataByIcao;
    private volatile Map<String, URI> logoUriByIata;

    public CachedAirlineLogoProvider() {
        this(Path.of("data", "cache", "airline-logos"),
                new CachedHttpFile(PASSENGER_DATA_URI,
                        Path.of("data", "cache", "airline-logo-metadata", "dotmarn-passenger.json"),
                        Duration.ofDays(30), "application/json,text/json,text/plain"),
                new CachedHttpFile(OPENFLIGHTS_DATA_URI,
                        Path.of("data", "cache", "airline-logo-metadata", "openflights-airlines.dat"),
                        Duration.ofDays(30)),
                new ObjectMapper(),
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                CachedAirlineLogoProvider::kiwiLogoUri);
    }

    CachedAirlineLogoProvider(
            Path logoDirectory,
            TextDataSource passengerData,
            TextDataSource openFlightsData,
            ObjectMapper objectMapper,
            HttpClient httpClient) {
        this(logoDirectory, passengerData, openFlightsData, objectMapper, httpClient,
                CachedAirlineLogoProvider::kiwiLogoUri);
    }

    CachedAirlineLogoProvider(
            Path logoDirectory,
            TextDataSource passengerData,
            TextDataSource openFlightsData,
            ObjectMapper objectMapper,
            HttpClient httpClient,
            Function<String, URI> fallbackLogoUri) {
        this.logoDirectory = logoDirectory;
        this.passengerData = passengerData;
        this.openFlightsData = openFlightsData;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.fallbackLogoUri = fallbackLogoUri;
    }

    @Override
    public Optional<RgbFrame> findLogo(String airlineIcaoCode) {
        String icao = normalizeIcao(airlineIcaoCode);
        if (icao.isEmpty()) {
            return Optional.empty();
        }
        synchronized (logos) {
            return logos.computeIfAbsent(icao, this::loadOrDownloadLogo);
        }
    }

    private Optional<RgbFrame> loadOrDownloadLogo(String icao) {
        Path file = logoDirectory.resolve(icao + ".rgb");
        if (Files.isRegularFile(file)) {
            try {
                return Optional.of(RawRgbLogoStore.read(file));
            } catch (IOException exception) {
                return Optional.empty();
            }
        }
        try {
            String iata = loadIataByIcao().get(icao);
            if (iata == null) {
                return Optional.empty();
            }
            URI logoUri = loadLogoUriByIata().getOrDefault(iata, fallbackLogoUri.apply(iata));
            RgbFrame frame = PngLogoDecoder.decodeAndFit(download(logoUri));
            RawRgbLogoStore.write(file, frame);
            return Optional.of(frame);
        } catch (IOException exception) {
            return Optional.empty();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        }
    }

    private Map<String, String> loadIataByIcao() throws IOException, InterruptedException {
        Map<String, String> loaded = iataByIcao;
        if (loaded != null) {
            return loaded;
        }
        synchronized (this) {
            if (iataByIcao == null) {
                Map<String, String> values = new HashMap<>();
                CsvReader.read(openFlightsData.get(), row -> {
                    if (row.size() >= 5) {
                        String iata = normalizeIata(row.get(3));
                        String icao = normalizeIcao(row.get(4));
                        if (!iata.isEmpty() && !icao.isEmpty()) {
                            values.putIfAbsent(icao, iata);
                        }
                    }
                });
                iataByIcao = Map.copyOf(values);
            }
            return iataByIcao;
        }
    }

    private Map<String, URI> loadLogoUriByIata() throws IOException, InterruptedException {
        Map<String, URI> loaded = logoUriByIata;
        if (loaded != null) {
            return loaded;
        }
        synchronized (this) {
            if (logoUriByIata == null) {
                Map<String, URI> values = new HashMap<>();
                JsonNode root = objectMapper.readTree(passengerData.get().toFile());
                if (!root.isArray()) {
                    throw new IOException("Airline logo metadata is not an array.");
                }
                for (JsonNode airline : root) {
                    String iata = normalizeIata(airline.path("iata").asText(""));
                    String logo = airline.path("logo").asText("").trim();
                    if (!iata.isEmpty() && !logo.isEmpty()) {
                        values.putIfAbsent(iata, URI.create(logo));
                    }
                }
                logoUriByIata = Map.copyOf(values);
            }
            return logoUriByIata;
        }
    }

    private static URI kiwiLogoUri(String iata) {
        return URI.create("https://images.kiwi.com/airlines/64/" + iata + ".png");
    }

    private byte[] download(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "image/png,image/*")
                .header("User-Agent", "screenduo-radar/0.1")
                .GET()
                .build();
        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException(uri.getHost() + " returned HTTP " + response.statusCode() + ".");
        }
        return response.body();
    }

    private static String normalizeIcao(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() != 3) {
            return "";
        }
        for (int index = 0; index < normalized.length(); index++) {
            if (!Character.isLetter(normalized.charAt(index))) {
                return "";
            }
        }
        return normalized;
    }

    private static String normalizeIata(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("\\N".equals(normalized) || normalized.length() < 2 || normalized.length() > 3) {
            return "";
        }
        return normalized;
    }
}
