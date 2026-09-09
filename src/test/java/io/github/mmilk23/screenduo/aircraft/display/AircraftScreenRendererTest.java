package io.github.mmilk23.screenduo.aircraft.display;

import static org.junit.jupiter.api.Assertions.*;

import io.github.mmilk23.screenduo.aircraft.NearbyAircraft;
import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.RgbFrame;
import io.github.mmilk23.screenduo.location.GeoPoint;
import java.time.Instant;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import io.github.mmilk23.screenduo.flight.FlightRoute;
import org.junit.jupiter.api.Test;

class AircraftScreenRendererTest {

    private static final DisplayGeometry GEOMETRY = new DisplayGeometry(320, 240);
    private static final Instant LOADED_AT = Instant.parse("2026-09-07T12:34:00Z");

    @Test
    void paginatesAndHighlightsSelectionOnLastPage() {
        NearbyAircraft aircraft = aircraft("TAM123", "LATAM", 1000.0, 400.0, false);
        RgbFrame frame = new AircraftListScreenRenderer().render(GEOMETRY,
                new AircraftListScreenData(List.of(aircraft, aircraft, aircraft, aircraft, aircraft),
                        4, LOADED_AT));

        assertEquals(320 * 240 * 3, frame.pixels().length);
        assertEquals(53, frame.redAt(9, 54));
        assertEquals(200, frame.greenAt(9, 54));
        assertEquals(255, frame.blueAt(9, 54));
        assertEquals(5, frame.redAt(9, 94));
    }

    @Test
    void rendersAirlineLogoOnListAndDetailsWhenAvailable() {
        NearbyAircraft aircraft = aircraft("TAM123", "LATAM", 1000.0, 400.0, false);
        RgbFrame logo = logo();
        RgbFrame plainList = new AircraftListScreenRenderer().render(GEOMETRY,
                new AircraftListScreenData(List.of(aircraft), 0, LOADED_AT));
        RgbFrame logoList = new AircraftListScreenRenderer().render(GEOMETRY,
                new AircraftListScreenData(List.of(aircraft), 0, LOADED_AT, Map.of("TAM", logo)));
        RgbFrame plainDetail = new AircraftDetailScreenRenderer().render(GEOMETRY,
                new AircraftDetailScreenData(aircraft, LOADED_AT, Optional.empty()));
        RgbFrame logoDetail = new AircraftDetailScreenRenderer().render(GEOMETRY,
                new AircraftDetailScreenData(aircraft, LOADED_AT, Optional.empty(), Optional.of(logo)));

        assertRegionDiffers(plainList, logoList, 13, 59, 38, 26);
        assertRegionDiffers(plainDetail, logoDetail, 274, 5, 38, 26);
        assertEquals(plainDetail.redAt(12, 43), logoDetail.redAt(12, 43));
    }

    @Test
    void rendersMissingDataAndGroundStatus() {
        NearbyAircraft missing = aircraft("", "", null, null, true);
        NearbyAircraft present = aircraft("TAM123", "LATAM", 1000.0, 400.0, false);
        AircraftDetailScreenRenderer renderer = new AircraftDetailScreenRenderer();
        RgbFrame missingFrame = renderer.render(GEOMETRY,
                new AircraftDetailScreenData(missing, LOADED_AT, Optional.empty()));
        RgbFrame presentFrame = renderer.render(GEOMETRY,
                new AircraftDetailScreenData(present, LOADED_AT, Optional.empty()));

        assertRegionDiffers(missingFrame, presentFrame, 12, 8, 290, 21);
        assertRegionDiffers(missingFrame, presentFrame, 12, 43, 290, 7);
        assertRegionDiffers(missingFrame, presentFrame, 116, 123, 192, 14);
        assertRegionDiffers(missingFrame, presentFrame, 116, 152, 192, 14);
        assertRegionDiffers(missingFrame, presentFrame, 12, 55, 100, 7);
    }

    @Test
    void fitsListAndDetailsToAnotherDisplayWithLetterboxing() {
        AircraftListScreenData data = new AircraftListScreenData(
                List.of(aircraft("TAM123", "LATAM", 1000.0, 400.0, false)), 0, LOADED_AT);
        DisplayGeometry wide = new DisplayGeometry(640, 360);
        for (RgbFrame frame : List.of(
                new AircraftListScreenRenderer().render(wide, data),
                new AircraftDetailScreenRenderer().render(wide,
                        new AircraftDetailScreenData(data.aircraft().getFirst(), LOADED_AT, Optional.empty())))) {
            assertEquals(wide, frame.geometry());
            assertEquals(640 * 360 * 3, frame.pixels().length);
            assertEquals(5, frame.redAt(0, 0));
            assertEquals(12, frame.redAt(80, 0));
            assertEquals(5, frame.redAt(639, 359));
        }
    }

    @Test
    void normalizesFallbacksUnitsAndBearing() {
        NearbyAircraft missing = aircraft("", "", null, null, true);
        assertEquals("abc123", AircraftScreenText.flight(missing));
        assertEquals("UNKNOWN OPERATOR", AircraftScreenText.airline(missing));
        assertEquals("N/A", AircraftScreenText.number(null, "%.0f M"));
        assertEquals("N/A", AircraftScreenText.number(Double.NaN, "%.0f M"));
        assertEquals("1234 M", AircraftScreenText.number(1234.0, "%.0f M"));
        assertEquals("450 KM/H", AircraftScreenText.number(450.0, "%.0f KM/H"));
        assertEquals("NE 045 DEG", AircraftScreenText.bearing(45.0));
        assertEquals("N 000 DEG", AircraftScreenText.bearing(359.9));
        assertEquals("W 270 DEG", AircraftScreenText.bearing(-90.0));
        assertEquals("SNAPSHOT 12:34 UTC", AircraftScreenText.snapshot(LOADED_AT));
        assertEquals("COMPANHIA...", AircraftScreenText.fit("Companhia Aerea Muito Longa", 12));
    }

    @Test
    void validatesSelectionAndCopiesInputList() {
        NearbyAircraft aircraft = aircraft("TAM123", "LATAM", null, null, false);
        java.util.ArrayList<NearbyAircraft> input = new java.util.ArrayList<>(List.of(aircraft));
        AircraftListScreenData data = new AircraftListScreenData(input, 0, LOADED_AT);
        input.clear();
        assertEquals(1, data.aircraft().size());
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftListScreenData(List.of(aircraft), 1, LOADED_AT));
        assertThrows(IllegalArgumentException.class,
                () -> new AircraftListScreenData(List.of(aircraft), -1, LOADED_AT));
        assertEquals(0, new AircraftListScreenData(List.of(), 10, LOADED_AT).selectedIndex());
    }

    @Test
    void displaysPlannedRouteAndUnavailableFallback() {
        NearbyAircraft aircraft = aircraft("TAM123", "LATAM", 1000.0, 400.0, false);
        var route = new FlightRoute("SBGR", "SBRJ",
                LOADED_AT.minusSeconds(1800), LOADED_AT.plusSeconds(1800),
                "Sao Paulo", "Rio de Janeiro");
        var renderer = new AircraftDetailScreenRenderer();
        RgbFrame known = renderer.render(GEOMETRY,
                new AircraftDetailScreenData(aircraft, LOADED_AT, Optional.of(route)));
        RgbFrame unknown = renderer.render(GEOMETRY,
                new AircraftDetailScreenData(aircraft, LOADED_AT, Optional.empty()));
        AircraftDetailScreenRenderer.RouteText routeText = AircraftDetailScreenRenderer.routeText(route);
        assertEquals("SAO PAULO > RIO DE JANEIRO", routeText.text());
        assertTrue(routeText.scale() <= 2);
        assertTrue(routeText.scale() >= 1);
        assertRegionDiffers(known, unknown, 12, 180, 200, 7);
        assertRegionDiffers(known, unknown, 12, 195, 288, 14);
        assertEquals(known.redAt(116, 65), unknown.redAt(116, 65));
    }


    @Test
    void keepsShortInternationalCityRoutesBeforeFallingBackToIcao() {
        var shortInternational = new FlightRoute("SBGL", "LFPG",
                LOADED_AT.minusSeconds(1800), LOADED_AT.plusSeconds(1800),
                "Rio de Janeiro", "Paris", "Rio de Janeiro / GIG", "Paris");
        var veryLong = new FlightRoute("SBBR", "SBRF",
                LOADED_AT.minusSeconds(1800), LOADED_AT.plusSeconds(1800),
                "Presidente Juscelino Kubitschek", "Recife Guararapes Gilberto Freyre");

        assertEquals("RIO DE JANEIRO / GIG > PARIS",
                AircraftDetailScreenRenderer.routeText(shortInternational).text());
        assertEquals("SBBR > SBRF", AircraftDetailScreenRenderer.routeText(veryLong).text());
    }

    @Test
    void includesIataWhenCityRouteWouldBeAmbiguous() {
        var route = new FlightRoute("KJFK", "LFPG",
                LOADED_AT.minusSeconds(1800), LOADED_AT.plusSeconds(1800),
                "New York", "Paris", "New York / JFK", "Paris");

        assertEquals("NEW YORK / JFK > PARIS", AircraftDetailScreenRenderer.routeText(route).text());
    }
    private static void assertRegionDiffers(
            RgbFrame first, RgbFrame second, int x, int y, int width, int height) {
        for (int row = y; row < y + height; row++) {
            for (int column = x; column < x + width; column++) {
                if (first.redAt(column, row) != second.redAt(column, row)
                        || first.greenAt(column, row) != second.greenAt(column, row)
                        || first.blueAt(column, row) != second.blueAt(column, row)) {
                    return;
                }
            }
        }
        fail("Expected the displayed field to change");
    }

    private static NearbyAircraft aircraft(
            String callsign, String airline, Double altitude, Double speed, boolean onGround) {
        return new NearbyAircraft("abc123", callsign, callsign.length() >= 3 ? callsign.substring(0, 3) : "", airline, "Brazil",
                new GeoPoint(-22.9, -43.2), 12.5, 45.0, altitude, speed, 90.0, onGround);
    }

    private static RgbFrame logo() {
        RgbFrame logo = new RgbFrame(new DisplayGeometry(32, 24));
        for (int y = 0; y < logo.geometry().height(); y++) {
            for (int x = 0; x < logo.geometry().width(); x++) {
                logo.setPixel(x, y, x < 16 ? 180 : 20, y < 12 ? 30 : 170, 40);
            }
        }
        return logo;
    }
}
