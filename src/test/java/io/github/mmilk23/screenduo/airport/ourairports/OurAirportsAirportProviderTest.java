package io.github.mmilk23.screenduo.airport.ourairports;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mmilk23.screenduo.airport.NearbyAirport;
import io.github.mmilk23.screenduo.location.GeoPoint;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OurAirportsAirportProviderTest {

    @TempDir
    Path temporaryDirectory;

    @Test
    void keepsRelevantAirportsAndIgnoresHeliports() throws IOException, InterruptedException {
        Path data = writeData("""
                1,NEAR,large_airport,Near Airport,0.1,0.0,100,AF,AA,AA-1,Town,yes,NEAR,NAR,,,
                2,HELI,heliport,Near Heliport,0.05,0.0,100,AF,AA,AA-1,Town,no,HELI,,,,
                3,FAR,large_airport,Far Airport,10.0,10.0,200,AF,AA,AA-1,Town,yes,FAR,FAR,,,
                """);
        OurAirportsAirportProvider provider =
                new OurAirportsAirportProvider(() -> data);

        List<NearbyAirport> result =
                provider.findNearby(new GeoPoint(0.0, 0.0), 50.0);

        assertEquals(1, result.size());
        assertEquals("NEAR", result.getFirst().ident());
        assertEquals(11.12, result.getFirst().distanceKm(), 0.02);
    }

    @Test
    void usesSmallAirportsWhenNoLargeOrMediumAirportIsNearby()
            throws IOException, InterruptedException {
        Path data = writeData("""
                1,SMALL,small_airport,Small Airport,0.1,0.0,100,AF,AA,AA-1,Town,no,SMAL,,,,
                2,HELI,heliport,Near Heliport,0.05,0.0,100,AF,AA,AA-1,Town,no,HELI,,,,
                """);
        OurAirportsAirportProvider provider =
                new OurAirportsAirportProvider(() -> data);

        List<NearbyAirport> result =
                provider.findNearby(new GeoPoint(0.0, 0.0), 50.0);

        assertEquals(1, result.size());
        assertEquals("SMALL", result.getFirst().ident());
    }

    private Path writeData(String rows) throws IOException {
        return Files.writeString(temporaryDirectory.resolve("airports.csv"),
                "id,ident,type,name,latitude_deg,longitude_deg,elevation_ft,continent,"
                        + "iso_country,iso_region,municipality,scheduled_service,gps_code,"
                        + "iata_code,local_code,home_link\n"
                        + rows);
    }
}
