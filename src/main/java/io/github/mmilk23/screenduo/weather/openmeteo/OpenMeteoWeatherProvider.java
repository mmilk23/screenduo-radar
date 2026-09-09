package io.github.mmilk23.screenduo.weather.openmeteo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mmilk23.screenduo.location.GeoPoint;
import io.github.mmilk23.screenduo.weather.WeatherConditions;
import io.github.mmilk23.screenduo.weather.WeatherProvider;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

public final class OpenMeteoWeatherProvider implements WeatherProvider {

    private static final String CURRENT_FIELDS = String.join(",",
            "temperature_2m", "relative_humidity_2m", "apparent_temperature",
            "precipitation", "weather_code", "is_day", "cloud_cover", "pressure_msl",
            "wind_speed_10m", "wind_direction_10m");

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OpenMeteoWeatherProvider() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                new ObjectMapper());
    }

    OpenMeteoWeatherProvider(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public WeatherConditions currentConditions(GeoPoint location)
            throws IOException, InterruptedException {
        URI uri = URI.create(String.format(Locale.ROOT,
                "https://api.open-meteo.com/v1/forecast"
                        + "?latitude=%.6f&longitude=%.6f&current=%s&daily=moon_phase&timezone=auto&timeformat=unixtime",
                location.latitude(), location.longitude(), CURRENT_FIELDS));
        JsonNode root = request(uri);
        JsonNode current = root.path("current");
        if (!current.isObject()) {
            throw new IOException("Open-Meteo response does not contain current conditions.");
        }

        return new WeatherConditions(
                instant(current, "time"),
                decimal(current, "temperature_2m"),
                decimal(current, "apparent_temperature"),
                integer(current, "relative_humidity_2m"),
                decimal(current, "precipitation"),
                integer(current, "weather_code"),
                dayFlag(current, "is_day"),
                firstDailyDecimal(root, "moon_phase"),
                integer(current, "cloud_cover"),
                decimal(current, "pressure_msl"),
                decimal(current, "wind_speed_10m"),
                decimal(current, "wind_direction_10m"));
    }

    private JsonNode request(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .header("User-Agent", "screenduo-radar/0.1")
                .GET()
                .build();
        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Open-Meteo returned HTTP " + response.statusCode() + ".");
        }
        return objectMapper.readTree(response.body());
    }

    private static Instant instant(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || !value.canConvertToLong()
                ? null : Instant.ofEpochSecond(value.longValue());
    }

    private static Double decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || !value.isNumber() ? null : value.doubleValue();
    }

    private static Double firstDailyDecimal(JsonNode root, String field) {
        JsonNode values = root.path("daily").path(field);
        if (!values.isArray() || values.isEmpty() || !values.get(0).isNumber()) {
            return null;
        }
        return values.get(0).doubleValue();
    }

    private static Boolean dayFlag(JsonNode node, String field) {
        Integer value = integer(node, field);
        return value == null ? null : value != 0;
    }

    private static Integer integer(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || !value.canConvertToInt() ? null : value.intValue();
    }
}
