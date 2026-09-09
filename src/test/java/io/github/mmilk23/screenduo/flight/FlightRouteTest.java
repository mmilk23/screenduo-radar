package io.github.mmilk23.screenduo.flight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class FlightRouteTest {

    private static final Instant DEPARTURE = Instant.parse("2026-09-09T12:00:00Z");
    private static final Instant ARRIVAL = Instant.parse("2026-09-09T13:00:00Z");

    @Test
    void fourArgumentConstructorUsesIcaoAsDisplayName() {
        FlightRoute route = new FlightRoute("SBGL", "SBSP", DEPARTURE, ARRIVAL);

        assertEquals("SBGL (SBGL)", route.originLabel());
        assertEquals("SBSP (SBSP)", route.destinationLabel());
        assertEquals("SBGL", route.originDisplayName());
        assertEquals("SBSP", route.destinationDisplayName());
    }

    @Test
    void trimsCitiesAndDisplayNamesAndBuildsLabels() {
        FlightRoute route = new FlightRoute(
                "SBGL", "SBSP", DEPARTURE, ARRIVAL,
                " Rio de Janeiro ", " Sao Paulo ", " Galeao ", " Congonhas ");

        assertEquals("Rio de Janeiro", route.originCity());
        assertEquals("Sao Paulo", route.destinationCity());
        assertEquals("Galeao", route.originDisplayName());
        assertEquals("Congonhas", route.destinationDisplayName());
        assertEquals("SBGL (Galeao)", route.originLabel());
        assertEquals("SBSP (Congonhas)", route.destinationLabel());
    }

    @Test
    void sixArgumentConstructorUsesCitiesAsDisplayNames() {
        FlightRoute route = new FlightRoute(
                "SBGL", "SBSP", DEPARTURE, ARRIVAL, "Rio", "Sao Paulo");

        assertEquals("SBGL (Rio)", route.originLabel());
        assertEquals("Sao Paulo", route.destinationDisplayName());
    }

    @Test
    void rejectsNullsInvalidIcaoAndNonIncreasingSchedule() {
        assertThrows(NullPointerException.class, () -> new FlightRoute(null, "SBSP", DEPARTURE, ARRIVAL));
        assertThrows(NullPointerException.class, () -> new FlightRoute("SBGL", null, DEPARTURE, ARRIVAL));
        assertThrows(NullPointerException.class, () -> new FlightRoute("SBGL", "SBSP", null, ARRIVAL));
        assertThrows(NullPointerException.class, () -> new FlightRoute("SBGL", "SBSP", DEPARTURE, null));
        assertThrows(NullPointerException.class, () -> new FlightRoute(
                "SBGL", "SBSP", DEPARTURE, ARRIVAL, null, "Sao Paulo", "Galeao", "Congonhas"));
        assertThrows(IllegalArgumentException.class, () -> new FlightRoute("GIG", "SBSP", DEPARTURE, ARRIVAL));
        assertThrows(IllegalArgumentException.class, () -> new FlightRoute("SBGL", "sbsp", DEPARTURE, ARRIVAL));
        assertThrows(IllegalArgumentException.class, () -> new FlightRoute("SBGL", "SBSP", DEPARTURE, DEPARTURE));
    }
}
