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

    @Test
    void findsCitiesByIcaoIdent() throws IOException, InterruptedException {
        Path data = writeData("""
                1,SBGL,large_airport,Rio Airport,-22.8,-43.2,28,SA,BR,BR-RJ,Rio de Janeiro,yes,SBGL,GIG,,,
                2,SBRJ,medium_airport,Santos Dumont Airport,-22.9,-43.1,11,SA,BR,BR-RJ,Rio de Janeiro,yes,SBRJ,SDU,,,
                3,LFPG,large_airport,Charles de Gaulle Airport,49.0,2.5,392,EU,FR,FR-IDF,"Paris (Roissy-en-France, Val-d'Oise)",yes,LFPG,CDG,,,
                4,SBNF,medium_airport,Navegantes Airport,-26.8,-48.6,18,SA,BR,BR-SC,Navegantes,yes,SBNF,NVT,,,
                """);
        OurAirportsAirportProvider provider =
                new OurAirportsAirportProvider(() -> data);

        assertEquals("Rio de Janeiro", provider.findCity(" sbgl ").orElseThrow());
        assertEquals("Navegantes", provider.findCity("SBNF").orElseThrow());
        assertEquals("Rio de Janeiro / GIG", provider.findAirportCity("SBGL").orElseThrow().displayName());
        assertEquals("Rio de Janeiro / SDU", provider.findAirportCity("SBRJ").orElseThrow().displayName());
        assertEquals("Paris", provider.findCity("LFPG").orElseThrow());
        assertEquals("Paris", provider.findAirportCity("LFPG").orElseThrow().displayName());
        assertEquals(java.util.Optional.empty(), provider.findCity("SBXX"));
    }


    @Test
    void ignoresNonScheduledAndSmallAirportsWhenDecidingCityAmbiguity()
            throws IOException, InterruptedException {
        Path data = writeData("""
                1,SBKP,large_airport,Viracopos Airport,-23.0,-47.1,2170,SA,BR,BR-SP,Campinas,yes,SBKP,VCP,,,
                2,SDAM,small_airport,Amarais Airport,-22.8,-47.1,2010,SA,BR,BR-SP,Campinas,no,SDAM,CPQ,,,
                3,SBVT,medium_airport,Vitoria Airport,-20.2,-40.3,11,SA,BR,BR-ES,Vitoria,yes,SBVT,VIX,,,
                4,SNXX,small_airport,Vitoria Small Airport,-20.3,-40.4,20,SA,BR,BR-ES,Vitoria,no,SNXX,,,,
                """);
        OurAirportsAirportProvider provider =
                new OurAirportsAirportProvider(() -> data);

        assertEquals("Campinas", provider.findAirportCity("SBKP").orElseThrow().displayName());
        assertEquals("Vitoria", provider.findAirportCity("SBVT").orElseThrow().displayName());
    }
    private Path writeData(String rows) throws IOException {
        return Files.writeString(temporaryDirectory.resolve("airports.csv"),
                "id,ident,type,name,latitude_deg,longitude_deg,elevation_ft,continent,"
                        + "iso_country,iso_region,municipality,scheduled_service,gps_code,"
                        + "iata_code,local_code,home_link\n"
                        + rows);
    }
}
