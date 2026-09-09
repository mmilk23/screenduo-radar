package io.github.mmilk23.screenduo.weather.openmeteo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mmilk23.screenduo.location.GeoPoint;
import io.github.mmilk23.screenduo.testsupport.StubHttpClient;
import io.github.mmilk23.screenduo.weather.WeatherConditions;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OpenMeteoWeatherProviderTest {

    private static final GeoPoint RIO = new GeoPoint(-22.9068, -43.1729);

    @Test
    void mapsCurrentConditionsAndBuildsExpectedRequest() throws Exception {
        StubHttpClient client = new StubHttpClient(200, """
                {
                  "current": {
                    "time": 1788973200,
                    "temperature_2m": 27.5,
                    "apparent_temperature": 29.2,
                    "relative_humidity_2m": 68,
                    "precipitation": 0.4,
                    "weather_code": 61,
                    "is_day": 1,
                    "cloud_cover": 72,
                    "pressure_msl": 1013.6,
                    "wind_speed_10m": 18.5,
                    "wind_direction_10m": 135.0
                  },
                  "daily": {"moon_phase": [0.42]}
                }
                """);
        OpenMeteoWeatherProvider provider = new OpenMeteoWeatherProvider(client, new ObjectMapper());

        WeatherConditions conditions = provider.currentConditions(RIO);

        assertEquals(Instant.ofEpochSecond(1788973200), conditions.observedAt());
        assertEquals(27.5, conditions.temperatureCelsius());
        assertEquals(29.2, conditions.apparentTemperatureCelsius());
        assertEquals(68, conditions.relativeHumidityPercent());
        assertEquals(0.4, conditions.precipitationMillimeters());
        assertEquals(61, conditions.weatherCode());
        assertTrue(conditions.isDay());
        assertEquals(0.42, conditions.moonPhase());
        assertEquals(72, conditions.cloudCoverPercent());
        assertEquals(1013.6, conditions.pressureHectopascals());
        assertEquals(18.5, conditions.windSpeedKilometersPerHour());
        assertEquals(135.0, conditions.windDirectionDegrees());

        String uri = client.lastRequest().uri().toString();
        assertTrue(uri.contains("latitude=-22.906800"));
        assertTrue(uri.contains("longitude=-43.172900"));
        assertTrue(uri.contains("current=temperature_2m"));
        assertTrue(uri.contains("daily=moon_phase"));
        assertEquals("application/json", client.lastRequest().headers().firstValue("Accept").orElseThrow());
    }

    @Test
    void mapsMissingAndInvalidOptionalFieldsToNull() throws Exception {
        StubHttpClient client = new StubHttpClient(200, """
                {
                  "current": {
                    "time": "not-a-number",
                    "temperature_2m": "warm",
                    "relative_humidity_2m": "humid",
                    "is_day": 0
                  },
                  "daily": {"moon_phase": []}
                }
                """);
        OpenMeteoWeatherProvider provider = new OpenMeteoWeatherProvider(client, new ObjectMapper());

        WeatherConditions conditions = provider.currentConditions(RIO);

        assertNull(conditions.observedAt());
        assertNull(conditions.temperatureCelsius());
        assertNull(conditions.apparentTemperatureCelsius());
        assertNull(conditions.relativeHumidityPercent());
        assertNull(conditions.precipitationMillimeters());
        assertNull(conditions.weatherCode());
        assertFalse(conditions.isDay());
        assertNull(conditions.moonPhase());
        assertNull(conditions.cloudCoverPercent());
    }

    @Test
    void returnsNullDayFlagAndMoonPhaseWhenDailyValueIsInvalid() throws Exception {
        StubHttpClient client = new StubHttpClient(200, """
                {"current": {}, "daily": {"moon_phase": ["unknown"]}}
                """);
        OpenMeteoWeatherProvider provider = new OpenMeteoWeatherProvider(client, new ObjectMapper());

        WeatherConditions conditions = provider.currentConditions(RIO);

        assertNull(conditions.isDay());
        assertNull(conditions.moonPhase());
    }

    @Test
    void rejectsMissingCurrentConditions() {
        StubHttpClient client = new StubHttpClient(200, "{\"daily\":{}}");
        OpenMeteoWeatherProvider provider = new OpenMeteoWeatherProvider(client, new ObjectMapper());

        IOException exception = assertThrows(IOException.class, () -> provider.currentConditions(RIO));
        assertTrue(exception.getMessage().contains("current conditions"));
    }

    @Test
    void rejectsNonSuccessfulHttpStatus() {
        StubHttpClient client = new StubHttpClient(429, "{}");
        OpenMeteoWeatherProvider provider = new OpenMeteoWeatherProvider(client, new ObjectMapper());

        IOException exception = assertThrows(IOException.class, () -> provider.currentConditions(RIO));
        assertTrue(exception.getMessage().contains("HTTP 429"));
    }

    @Test
    void rejectsMalformedJson() {
        StubHttpClient client = new StubHttpClient(200, "not-json");
        OpenMeteoWeatherProvider provider = new OpenMeteoWeatherProvider(client, new ObjectMapper());

        assertThrows(IOException.class, () -> provider.currentConditions(RIO));
    }
}
