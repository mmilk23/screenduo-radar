package io.github.mmilk23.screenduo.aircraft.opensky;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mmilk23.screenduo.aircraft.NearbyAircraft;
import io.github.mmilk23.screenduo.airline.AirlineResolver;
import io.github.mmilk23.screenduo.location.GeoPoint;
import io.github.mmilk23.screenduo.testsupport.StubHttpClient;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OpenSkyAircraftProviderTest {

    private static final GeoPoint CENTER = new GeoPoint(-22.9068, -43.1729);

    @Test
    void mapsFiltersAndSortsAircraft() throws Exception {
        StubHttpClient client = new StubHttpClient(200, """
                {
                  "states": [
                    ["far001", "TAM999 ", "Brazil", null, null, -42.0, -22.0, 9000, false, 200, 90, null, null, 9500, null, null, null],
                    ["near02", "AZU456 ", "Brazil", null, null, -43.16, -22.90, 3000, false, 100, 270, null, null, 3200, null, null, null],
                    ["near01", "GLO123 ", "Brazil", null, null, -43.17, -22.905, 2000, true, 120, 180, null, null, 2100, null, null, null],
                    ["bad", "BAD", "Brazil"],
                    ["nopos", "TAM111", "Brazil", null, null, null, null, null, false, null, null, null, null, null, null, null, null]
                  ]
                }
                """);
        AirlineResolver resolver = callsign -> Optional.of("Airline " + callsign.trim().substring(0, 3));
        OpenSkyAircraftProvider provider = new OpenSkyAircraftProvider(client, new ObjectMapper(), resolver);

        List<NearbyAircraft> aircraft = provider.findNearby(CENTER, 20.0);

        assertEquals(2, aircraft.size());
        assertEquals("near01", aircraft.get(0).icao24());
        assertEquals("GLO123", aircraft.get(0).callsign());
        assertEquals("GLO", aircraft.get(0).airlineIcaoCode());
        assertEquals("Airline GLO", aircraft.get(0).airlineName());
        assertEquals(2100.0, aircraft.get(0).altitudeMeters());
        assertEquals(432.0, aircraft.get(0).speedKilometersPerHour(), 0.0001);
        assertEquals(180.0, aircraft.get(0).trackDegrees());
        assertTrue(aircraft.get(0).onGround());
        assertTrue(aircraft.get(0).distanceKm() <= aircraft.get(1).distanceKm());

        String uri = client.lastRequest().uri().toString();
        assertTrue(uri.startsWith("https://opensky-network.org/api/states/all?"));
        assertTrue(uri.contains("lamin="));
        assertTrue(uri.contains("lomin="));
        assertEquals("application/json", client.lastRequest().headers().firstValue("Accept").orElseThrow());
    }

    @Test
    void fallsBackToBarometricAltitudeAndHandlesOptionalNumbers() throws Exception {
        StubHttpClient client = new StubHttpClient(200, """
                {"states":[["abc123","12X ","Test",null,null,-43.17,-22.905,1500,false,null,null,null,null,null,null,null,null]]}
                """);
        OpenSkyAircraftProvider provider = new OpenSkyAircraftProvider(
                client, new ObjectMapper(), callsign -> Optional.empty());

        NearbyAircraft aircraft = provider.findNearby(CENTER, 20.0).getFirst();

        assertEquals("", aircraft.airlineIcaoCode());
        assertEquals("", aircraft.airlineName());
        assertEquals(1500.0, aircraft.altitudeMeters());
        assertNull(aircraft.speedKilometersPerHour());
        assertNull(aircraft.trackDegrees());
        assertFalse(aircraft.onGround());
    }

    @Test
    void handlesNullOrMissingStatesAsEmptyList() throws Exception {
        OpenSkyAircraftProvider nullStates = providerFor("{\"states\":null}");
        OpenSkyAircraftProvider missingStates = providerFor("{}");

        assertTrue(nullStates.findNearby(CENTER, 20.0).isEmpty());
        assertTrue(missingStates.findNearby(CENTER, 20.0).isEmpty());
    }

    @Test
    void rejectsInvalidStatesField() {
        OpenSkyAircraftProvider provider = providerFor("{\"states\":{}}");

        IOException exception = assertThrows(IOException.class, () -> provider.findNearby(CENTER, 20.0));
        assertTrue(exception.getMessage().contains("invalid states"));
    }

    @Test
    void rejectsNonSuccessfulHttpStatusAndMalformedJson() {
        StubHttpClient limitedClient = new StubHttpClient(429, "{}");
        OpenSkyAircraftProvider limited = new OpenSkyAircraftProvider(
                limitedClient, new ObjectMapper(), callsign -> Optional.empty());
        assertTrue(assertThrows(IOException.class, () -> limited.findNearby(CENTER, 20.0))
                .getMessage().contains("HTTP 429"));

        OpenSkyAircraftProvider malformed = providerFor("not-json");
        assertThrows(IOException.class, () -> malformed.findNearby(CENTER, 20.0));
    }

    @Test
    void propagatesAirlineResolverFailure() {
        StubHttpClient client = new StubHttpClient(200, """
                {"states":[["abc123","TAM123","Brazil",null,null,-43.17,-22.905,1500,false,100,90,null,null,1600,null,null,null]]}
                """);
        AirlineResolver resolver = callsign -> { throw new IOException("resolver failed"); };
        OpenSkyAircraftProvider provider = new OpenSkyAircraftProvider(client, new ObjectMapper(), resolver);

        IOException exception = assertThrows(IOException.class, () -> provider.findNearby(CENTER, 20.0));
        assertEquals("resolver failed", exception.getMessage());
    }

    private static OpenSkyAircraftProvider providerFor(String body) {
        return new OpenSkyAircraftProvider(
                new StubHttpClient(200, body), new ObjectMapper(), callsign -> Optional.empty());
    }
}
