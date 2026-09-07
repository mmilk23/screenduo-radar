package io.github.mmilk23.screenduo.aircraft.opensky;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mmilk23.screenduo.aircraft.AircraftProvider;
import io.github.mmilk23.screenduo.aircraft.NearbyAircraft;
import io.github.mmilk23.screenduo.location.BoundingBox;
import io.github.mmilk23.screenduo.location.GeoMath;
import io.github.mmilk23.screenduo.location.GeoPoint;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class OpenSkyAircraftProvider implements AircraftProvider {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenSkyAircraftProvider() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                new ObjectMapper());
    }

    OpenSkyAircraftProvider(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<NearbyAircraft> findNearby(GeoPoint center, double radiusKm)
            throws IOException, InterruptedException {
        BoundingBox box = GeoMath.boundingBox(center, radiusKm);
        URI uri = URI.create(String.format(Locale.ROOT,
                "https://opensky-network.org/api/states/all"
                        + "?lamin=%.6f&lomin=%.6f&lamax=%.6f&lomax=%.6f",
                box.minimumLatitude(), box.minimumLongitude(),
                box.maximumLatitude(), box.maximumLongitude()));
        JsonNode states = request(uri).path("states");
        if (states.isMissingNode() || states.isNull()) {
            return List.of();
        }
        if (!states.isArray()) {
            throw new IOException("OpenSky response contains an invalid states field.");
        }

        List<NearbyAircraft> aircraft = new ArrayList<>();
        for (JsonNode state : states) {
            NearbyAircraft nearby = map(center, state);
            if (nearby != null && nearby.distanceKm() <= radiusKm) {
                aircraft.add(nearby);
            }
        }
        aircraft.sort(Comparator.comparingDouble(NearbyAircraft::distanceKm));
        return List.copyOf(aircraft);
    }

    private JsonNode request(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("Accept", "application/json")
                .header("User-Agent", "screenduo-radar/0.1")
                .GET()
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("OpenSky returned HTTP " + response.statusCode()
                    + ". Anonymous access may be rate limited.");
        }
        return objectMapper.readTree(response.body());
    }

    private static NearbyAircraft map(GeoPoint center, JsonNode state) {
        if (!state.isArray() || state.size() < 17
                || !state.path(5).isNumber() || !state.path(6).isNumber()) {
            return null;
        }
        GeoPoint position = new GeoPoint(state.path(6).doubleValue(), state.path(5).doubleValue());
        double distance = GeoMath.distanceKm(center, position);
        Double altitude = number(state, 13);
        if (altitude == null) {
            altitude = number(state, 7);
        }
        Double velocity = number(state, 9);

        return new NearbyAircraft(
                text(state, 0),
                text(state, 1),
                text(state, 2),
                position,
                distance,
                GeoMath.bearingDegrees(center, position),
                altitude,
                velocity == null ? null : velocity * 3.6,
                number(state, 10),
                state.path(8).asBoolean(false));
    }

    private static String text(JsonNode state, int index) {
        JsonNode value = state.path(index);
        return value.isTextual() ? value.textValue().trim() : "";
    }

    private static Double number(JsonNode state, int index) {
        JsonNode value = state.path(index);
        return value.isNumber() ? value.doubleValue() : null;
    }
}
