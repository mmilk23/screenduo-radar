package io.github.mmilk23.screenduo.flight.display;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.flight.FlightRoute;
import io.github.mmilk23.screenduo.flight.ScheduledFlight;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AirportFlightBoardRendererTest {
    private static final LocalDate DATE = LocalDate.of(2026, 9, 7);

    @Test
    void filtersByAirportEventDateAndSortsByArrivalTime() {
        var late = flight(3);
        var early = flight(1);
        var overnight = new ScheduledFlight("TAP1", new FlightRoute("LPPT", "SBRJ",
                Instant.parse("2026-09-06T21:00:00Z"), Instant.parse("2026-09-07T06:00:00Z")));
        var data = new AirportFlightBoardData("SBRJ", DATE,
                AirportFlightBoardData.Direction.ARRIVALS, List.of(late, early, early, overnight), 0);
        assertEquals(List.of(early, late, overnight), data.flights());
        assertTrue(new AirportFlightBoardData("SBRJ", DATE,
                AirportFlightBoardData.Direction.DEPARTURES, data.flights(), 0).flights().isEmpty());
    }

    @Test
    void rendersLastPageAndFitsWideDisplay() {
        var flights = IntStream.rangeClosed(1, 6).mapToObj(AirportFlightBoardRendererTest::flight).toList();
        var data = new AirportFlightBoardData("SBRJ", DATE,
                AirportFlightBoardData.Direction.ARRIVALS, flights, 5);
        var renderer = new AirportFlightBoardRenderer();
        var frame = renderer.render(new DisplayGeometry(320, 240), data);
        assertEquals(53, frame.redAt(9, 58));
        assertEquals(200, frame.greenAt(9, 58));
        assertEquals(5, frame.redAt(9, 87));
        var wide = renderer.render(new DisplayGeometry(640, 360), data);
        assertEquals(640 * 360 * 3, wide.pixels().length);
        assertEquals(5, wide.redAt(0, 0));
        assertEquals(12, wide.redAt(80, 0));
    }

    @Test
    void rendersEmptyBoardAndRejectsInvalidSelection() {
        var empty = new AirportFlightBoardData("SBRJ", DATE,
                AirportFlightBoardData.Direction.ARRIVALS, List.of(), 8);
        assertEquals(0, empty.selectedIndex());
        assertEquals(320 * 240 * 3, new AirportFlightBoardRenderer()
                .render(new DisplayGeometry(320, 240), empty).pixels().length);
        assertThrows(IllegalArgumentException.class, () -> new AirportFlightBoardData("SBRJ", DATE,
                AirportFlightBoardData.Direction.ARRIVALS, List.of(flight(1)), 1));
    }

    private static ScheduledFlight flight(int hour) {
        var departure = DATE.atStartOfDay().toInstant(java.time.ZoneOffset.UTC).plusSeconds(hour * 3600L);
        return new ScheduledFlight("TAM" + hour,
                new FlightRoute("SBGR", "SBRJ", departure, departure.plusSeconds(1800)));
    }
}