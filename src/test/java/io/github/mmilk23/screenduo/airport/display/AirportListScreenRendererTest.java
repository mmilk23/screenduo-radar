package io.github.mmilk23.screenduo.airport.display;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mmilk23.screenduo.airport.NearbyAirport;
import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.RgbFrame;
import io.github.mmilk23.screenduo.location.GeoPoint;
import java.util.List;
import org.junit.jupiter.api.Test;

class AirportListScreenRendererTest {

    @Test
    void rendersSelectedAirportAndPagination() {
        List<NearbyAirport> airports = List.of(
                airport("GIG", 10.4),
                airport("SDU", 12.8),
                airport("JAC", 18.1),
                airport("RIO", 25.7),
                airport("ITB", 42.3),
                airport("QNV", 48.9));

        RgbFrame frame = new AirportListScreenRenderer().render(
                new DisplayGeometry(320, 240),
                new AirportListScreenData(airports, 5));

        assertEquals(320 * 240 * 3, frame.pixels().length);
        assertEquals(53, frame.redAt(20, 40));
        assertEquals(200, frame.greenAt(20, 40));
        assertEquals(255, frame.blueAt(20, 40));
    }

    @Test
    void rendersEmptyList() {
        RgbFrame frame = new AirportListScreenRenderer().render(
                new DisplayGeometry(320, 240),
                new AirportListScreenData(List.of(), 0));

        assertEquals(320 * 240 * 3, frame.pixels().length);
    }

    private static NearbyAirport airport(String code, double distance) {
        return new NearbyAirport(
                code,
                code,
                code + " Airport",
                "Rio de Janeiro",
                "BR",
                "medium_airport",
                new GeoPoint(-22.9, -43.2),
                distance,
                45.0,
                5.0);
    }
}
