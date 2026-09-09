package io.github.mmilk23.screenduo.flight.siros;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.mmilk23.screenduo.flight.FlightRoute;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SirosFlightRouteProviderTest {

    private static final Instant REFERENCE = Instant.parse("2026-09-07T12:30:00Z");

    @TempDir
    Path temporaryDirectory;

    @Test
    void readsWrappedJsonAndNormalizesCallsignAndLeadingZeros() throws Exception {
        var provider = provider(List.of(row("TAM", "0034", "SBGR", "SBRJ",
                "07/09/2026 12:00", "07/09/2026 13:00")), true);
        FlightRoute route = provider.findRoute(" tam34 ", REFERENCE).orElseThrow();

        assertEquals("SBGR", route.originIcao());
        assertEquals("SBRJ", route.destinationIcao());
        assertEquals(Instant.parse("2026-09-07T12:00:00Z"), route.scheduledDeparture());
        assertEquals(Instant.parse("2026-09-07T13:00:00Z"), route.scheduledArrival());
    }

    @Test
    void readsArrayJsonAndDistinguishesOperators() throws Exception {
        var provider = provider(List.of(
                row("TAM", "0034", "SBGR", "SBRJ", "07/09/2026 12:00", "07/09/2026 13:00"),
                row("GLO", "0034", "SBSP", "SBBR", "07/09/2026 12:00", "07/09/2026 13:00")), false);

        assertEquals("SBSP", provider.findRoute("GLO0034", REFERENCE).orElseThrow().originIcao());
        assertTrue(provider.findRoute("AZU34", REFERENCE).isEmpty());
    }

    @Test
    void matchesPreviousDayDepartureAcrossMidnightAndQueriesAdjacentDays() throws Exception {
        var provider = provider(List.of(row("TAP", "0005", "LPPT", "SBSG",
                "06/09/2026 21:00", "07/09/2026 05:00")), false);

        assertTrue(provider.findRoute("TAP5", Instant.parse("2026-09-07T00:30:00Z")).isPresent());
        assertEquals("dataReferenciaInicio=06092026&dataReferenciaFinal=08092026",
                SirosFlightRouteProvider.scheduleUri(LocalDate.of(2026, 9, 7)).getQuery());
    }

    @Test
    void selectsOnlyTheDateAndStageWhoseTimeWindowMatches() throws Exception {
        var provider = provider(List.of(
                row("TAM", "0034", "SBGR", "SBRJ", "06/09/2026 12:00", "06/09/2026 13:00"),
                row("TAM", "0034", "SBGR", "SBRJ", "07/09/2026 06:00", "07/09/2026 07:00"),
                row("TAM", "0034", "SBRJ", "SBBR", "07/09/2026 12:00", "07/09/2026 13:00")), false);

        assertEquals("SBRJ", provider.findRoute("TAM34", REFERENCE).orElseThrow().originIcao());
    }

    @Test
    void refusesAmbiguousStagesWithinTolerance() throws Exception {
        var provider = provider(List.of(
                row("TAM", "0034", "SBGR", "SBRJ", "07/09/2026 10:00", "07/09/2026 11:00"),
                row("TAM", "0034", "SBRJ", "SBBR", "07/09/2026 12:00", "07/09/2026 13:00")), false);

        assertTrue(provider.findRoute("TAM34", REFERENCE).isEmpty());
    }

    @Test
    void toleratesTwoHoursButDoesNotGuessAfterLongDelays() throws Exception {
        var provider = provider(List.of(row("TAM", "0034", "SBGR", "SBRJ",
                "07/09/2026 12:00", "07/09/2026 13:00")), false);

        assertTrue(provider.findRoute("TAM34", Instant.parse("2026-09-07T10:00:00Z")).isPresent());
        assertTrue(provider.findRoute("TAM34", Instant.parse("2026-09-07T15:00:00Z")).isPresent());
        assertTrue(provider.findRoute("TAM34", Instant.parse("2026-09-07T15:00:01Z")).isEmpty());
        assertTrue(provider.findRoute("TAM34", Instant.parse("2026-09-07T09:59:59Z")).isEmpty());
    }

    @Test
    void ignoresIdenticalDuplicateRegistrations() throws Exception {
        var row = row("TAM", "0034", "SBGR", "SBRJ",
                "07/09/2026 12:00", "07/09/2026 13:00");
        assertTrue(provider(List.of(row, row), false).findRoute("TAM34", REFERENCE).isPresent());
    }

    @Test
    void doesNotUseCodeshareNumbersAsOperatingCallsigns() throws Exception {
        var row = new java.util.HashMap<>(row("TAM", "0034", "SBGR", "SBRJ",
                "07/09/2026 12:00", "07/09/2026 13:00"));
        row.put("tx_codeshare", "AZU/1234");

        assertTrue(provider(List.of(row), false).findRoute("AZU1234", REFERENCE).isEmpty());
    }

    @Test
    void skipsInvalidOrNonNumericCallsignsWithoutLoadingSchedule() throws Exception {
        var provider = new SirosFlightRouteProvider(date -> {
            throw new AssertionError("Invalid callsigns must not trigger a download");
        });
        for (String callsign : List.of("", "PRABC", "TAM12AB", "TAM", "TAM0000", "TAM12345")) {
            assertTrue(provider.findRoute(callsign, REFERENCE).isEmpty());
        }
        assertTrue(provider.findRoute(null, REFERENCE).isEmpty());
    }

    @Test
    void ignoresMalformedRowsAndRejectsInvalidResponseShapes() throws Exception {
        var invalid = List.of(
                row("TAM0", "034", "SBGR", "SBRJ", "07/09/2026 12:00", "07/09/2026 13:00"),
                row("TAM", "34", "", "SBRJ", "07/09/2026 12:00", "07/09/2026 13:00"),
                row("TAM", "34", "SBGR", "SBRJ", "31/02/2026 12:00", "07/09/2026 13:00"),
                row("TAM", "34", "SBGR", "SBRJ", "07/09/2026 14:00", "07/09/2026 13:00"));
        assertTrue(provider(invalid, false).findRoute("TAM34", REFERENCE).isEmpty());
        for (String payload : List.of("null", "{}", "<html>Error</html>", "\"invalid\"")) {
            Path file = Files.createTempFile(temporaryDirectory, "invalid", ".json");
            Files.writeString(file, payload);
            assertThrows(IOException.class,
                    () -> new SirosFlightRouteProvider(date -> () -> file).findRoute("TAM34", REFERENCE));
        }
    }

    @Test
    void reusesScheduleWithinSnapshotDateAndReloadsForAnotherDate() throws Exception {
        Path file = Files.writeString(temporaryDirectory.resolve("empty.json"), "[]");
        AtomicInteger reads = new AtomicInteger();
        var provider = new SirosFlightRouteProvider(date -> () -> {
            reads.incrementAndGet();
            return file;
        });
        provider.findRoute("TAM34", REFERENCE);
        provider.findRoute("GLO1234", REFERENCE);
        assertEquals(1, reads.get());
        provider.findRoute("TAM34", REFERENCE.plusSeconds(86400));
        assertEquals(2, reads.get());
    }

    @Test
    void propagatesSourceFailureAndInterruption() {
        assertThrows(IOException.class, () -> new SirosFlightRouteProvider(date -> () -> {
            throw new IOException("Offline");
        }).findRoute("TAM34", REFERENCE));
        assertThrows(InterruptedException.class, () -> new SirosFlightRouteProvider(date -> () -> {
            throw new InterruptedException("Cancelled");
        }).findRoute("TAM34", REFERENCE));
    }

    @Test
    void findsAirportArrivalsAndDeparturesOnTheRequestedUtcDay() throws Exception {
        var provider = provider(List.of(
                row("TAP", "5", "LPPT", "SBRJ", "06/09/2026 21:00", "07/09/2026 05:00"),
                row("TAM", "34", "SBRJ", "SBGR", "07/09/2026 23:00", "08/09/2026 00:10"),
                row("TAM", "35", "SBRJ", "SBGR", "08/09/2026 08:00", "08/09/2026 09:10"),
                row("GLO", "12", "SBBR", "SBCF", "07/09/2026 12:00", "07/09/2026 13:00")),
                true);
        var flights = provider.findFlights(" sbrj ", LocalDate.of(2026, 9, 7));
        assertEquals(List.of("TAP5", "TAM34"),
                flights.stream().map(io.github.mmilk23.screenduo.flight.ScheduledFlight::callsign).toList());
    }

    @Test
    void sharesParsedScheduleBetweenRouteAndAirportQueries() throws Exception {
        var rows = List.of(row("TAM", "34", "SBGR", "SBRJ",
                "07/09/2026 12:00", "07/09/2026 13:00"));
        Path file = Files.writeString(temporaryDirectory.resolve("shared.json"),
                new ObjectMapper().writeValueAsString(rows));
        AtomicInteger reads = new AtomicInteger();
        var provider = new SirosFlightRouteProvider(date -> () -> {
            reads.incrementAndGet();
            return file;
        });
        assertTrue(provider.findRoute("TAM34", REFERENCE).isPresent());
        assertEquals(1, provider.findFlights("SBGR", LocalDate.of(2026, 9, 7)).size());
        assertEquals(1, reads.get());
    }

    @Test
    void invalidAirportCodeDoesNotDownloadData() throws Exception {
        var provider = new SirosFlightRouteProvider(date -> {
            throw new AssertionError("Must not download for a non-ICAO airport identifier");
        });
        assertTrue(provider.findFlights("BR-1234", LocalDate.of(2026, 9, 7)).isEmpty());
    }

    private SirosFlightRouteProvider provider(List<Map<String, String>> rows, boolean wrapped)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(rows);
        Path file = Files.createTempFile(temporaryDirectory, "siros", ".json");
        Files.writeString(file, wrapped ? mapper.writeValueAsString(json) : json);
        return new SirosFlightRouteProvider(date -> () -> file);
    }

    private static Map<String, String> row(
            String operator, String number, String origin, String destination,
            String departure, String arrival) {
        return Map.of("sg_empresa_icao", operator, "nr_voo", number, "nr_etapa", "1",
                "sg_icao_origem", origin, "sg_icao_destino", destination,
                "dt_partida_prevista_utc", departure, "dt_chegada_prevista_utc", arrival);
    }
}