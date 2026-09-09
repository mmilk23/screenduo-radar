package io.github.mmilk23.screenduo.flight.display;

import io.github.mmilk23.screenduo.flight.ScheduledFlight;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record AirportFlightBoardData(
        String airportIcao, LocalDate date, Direction direction,
        List<ScheduledFlight> flights, int selectedIndex) {

    public enum Direction { ARRIVALS, DEPARTURES }

    public AirportFlightBoardData {
        Objects.requireNonNull(airportIcao, "airportIcao");
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(direction, "direction");
        String code = airportIcao;
        LocalDate day = date;
        Direction tab = direction;
        flights = flights.stream()
                .filter(flight -> (tab == Direction.ARRIVALS
                        ? flight.route().destinationIcao() : flight.route().originIcao()).equals(code))
                .filter(flight -> eventTime(flight, tab).atZone(ZoneOffset.UTC).toLocalDate().equals(day))
                .distinct()
                .sorted(Comparator.comparing((ScheduledFlight flight) -> eventTime(flight, tab))
                        .thenComparing(ScheduledFlight::callsign)
                        .thenComparing(flight -> flight.route().originIcao())
                        .thenComparing(flight -> flight.route().destinationIcao()))
                .toList();
        if (flights.isEmpty()) {
            selectedIndex = 0;
        } else if (selectedIndex < 0 || selectedIndex >= flights.size()) {
            throw new IllegalArgumentException("Selected flight index is outside the board.");
        }
    }

    public static Instant eventTime(ScheduledFlight flight, Direction direction) {
        return direction == Direction.ARRIVALS
                ? flight.route().scheduledArrival() : flight.route().scheduledDeparture();
    }
}