package io.github.mmilk23.screenduo.flight.siros;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mmilk23.screenduo.data.CachedHttpFile;
import io.github.mmilk23.screenduo.data.TextDataSource;
import io.github.mmilk23.screenduo.flight.FlightRoute;
import io.github.mmilk23.screenduo.flight.AirportFlightProvider;
import io.github.mmilk23.screenduo.flight.ScheduledFlight;
import io.github.mmilk23.screenduo.flight.FlightRouteProvider;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;

/**
 * Resolves planned routes from ANAC registrations for operations involving Brazilian territory.
 * This source is not a worldwide route service and does not report actual departures or diversions.
 */
public final class SirosFlightRouteProvider implements FlightRouteProvider, AirportFlightProvider {

    private static final Duration CACHE_AGE = Duration.ofHours(6);
    private static final Duration SCHEDULE_TOLERANCE = Duration.ofHours(2);
    private static final DateTimeFormatter QUERY_DATE =
            DateTimeFormatter.ofPattern("ddMMuuuu", Locale.ROOT);
    private static final DateTimeFormatter SCHEDULE_TIME =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm", Locale.ROOT)
                    .withResolverStyle(ResolverStyle.STRICT);

    private final Function<LocalDate, TextDataSource> sources;
    private final ObjectMapper mapper = new ObjectMapper();
    private LocalDate loadedDate;
    private List<ScheduledFlight> schedule;

    public SirosFlightRouteProvider() {
        this(date -> new CachedHttpFile(
                scheduleUri(date),
                Path.of("data", "cache", "siros", "flights-" + date + ".json"),
                CACHE_AGE,
                "application/json"));
    }

    SirosFlightRouteProvider(Function<LocalDate, TextDataSource> sources) {
        this.sources = sources;
    }

    @Override
    public Optional<FlightRoute> findRoute(String callsign, Instant referenceTime)
            throws IOException, InterruptedException {
        String key = flightKey(callsign);
        if (key == null) {
            return Optional.empty();
        }
        LocalDate date = referenceTime.atZone(ZoneOffset.UTC).toLocalDate();
        List<FlightRoute> candidates = loadSchedule(date).stream()
                .filter(flight -> flight.callsign().equals(key))
                .map(ScheduledFlight::route)
                .filter(route -> !referenceTime.isBefore(
                        route.scheduledDeparture().minus(SCHEDULE_TOLERANCE))
                        && !referenceTime.isAfter(
                                route.scheduledArrival().plus(SCHEDULE_TOLERANCE)))
                .distinct()
                .toList();
        return candidates.size() == 1 ? Optional.of(candidates.getFirst()) : Optional.empty();
    }

    @Override
    public List<ScheduledFlight> findFlights(String airportIcao, LocalDate utcDate)
            throws IOException, InterruptedException {
        String code = airportIcao.trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z]{4}")) {
            return List.of();
        }
        return loadSchedule(utcDate).stream()
                .filter(flight -> (flight.route().originIcao().equals(code)
                        && flight.route().scheduledDeparture().atZone(ZoneOffset.UTC)
                                .toLocalDate().equals(utcDate))
                        || (flight.route().destinationIcao().equals(code)
                        && flight.route().scheduledArrival().atZone(ZoneOffset.UTC)
                                .toLocalDate().equals(utcDate)))
                .distinct()
                .toList();
    }

    static URI scheduleUri(LocalDate date) {
        return URI.create("https://sas.anac.gov.br/sas/siros_api/api/voosPeriodo"
                + "?dataReferenciaInicio=" + QUERY_DATE.format(date.minusDays(1))
                + "&dataReferenciaFinal=" + QUERY_DATE.format(date.plusDays(1)));
    }

    private List<ScheduledFlight> loadSchedule(LocalDate date)
            throws IOException, InterruptedException {
        if (date.equals(loadedDate)) {
            return schedule;
        }
        JsonNode root = mapper.readTree(sources.apply(date).get().toFile());
        // SIROS currently wraps its JSON array inside a JSON string.
        if (root != null && root.isTextual()) {
            root = mapper.readTree(root.textValue());
        }
        if (root == null || !root.isArray()) {
            throw new IOException("SIROS response must contain a flight array.");
        }

        List<ScheduledFlight> flights = new ArrayList<>();
        for (JsonNode row : root) {
            String operator = text(row, "sg_empresa_icao").toUpperCase(Locale.ROOT);
            String number = text(row, "nr_voo");
            if (!operator.matches("[A-Z]{3}") || !number.matches("[0-9]{1,4}")) {
                continue;
            }
            String key = flightKey(operator + number);
            String origin = text(row, "sg_icao_origem").toUpperCase(Locale.ROOT);
            String destination = text(row, "sg_icao_destino").toUpperCase(Locale.ROOT);
            if (key == null || !origin.matches("[A-Z]{4}") || !destination.matches("[A-Z]{4}")) {
                continue;
            }
            try {
                Instant departure = LocalDateTime.parse(
                        text(row, "dt_partida_prevista_utc"), SCHEDULE_TIME).toInstant(ZoneOffset.UTC);
                Instant arrival = LocalDateTime.parse(
                        text(row, "dt_chegada_prevista_utc"), SCHEDULE_TIME).toInstant(ZoneOffset.UTC);
                if (arrival.isAfter(departure)) {
                    flights.add(new ScheduledFlight(key, new FlightRoute(
                            origin, destination, departure, arrival)));
                }
            } catch (DateTimeParseException exception) {
                // Incomplete schedule rows cannot identify a flight safely.
            }
        }
        schedule = List.copyOf(flights);
        loadedDate = date;
        return schedule;
    }

    private static String text(JsonNode row, String field) {
        JsonNode value = row.path(field);
        return value.isTextual() ? value.textValue().trim() : "";
    }

    private static String flightKey(String callsign) {
        if (callsign == null) {
            return null;
        }
        String normalized = callsign.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}[0-9]{1,4}")) {
            return null;
        }
        int number = Integer.parseInt(normalized.substring(3));
        return number == 0 ? null : normalized.substring(0, 3) + number;
    }

}