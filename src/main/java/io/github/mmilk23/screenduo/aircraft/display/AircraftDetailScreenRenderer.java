package io.github.mmilk23.screenduo.aircraft.display;

import io.github.mmilk23.screenduo.aircraft.NearbyAircraft;
import io.github.mmilk23.screenduo.display.CompactPixelText;
import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.FrameCanvas;
import io.github.mmilk23.screenduo.display.FrameScaler;
import io.github.mmilk23.screenduo.display.RgbColor;
import io.github.mmilk23.screenduo.display.RgbFrame;
import io.github.mmilk23.screenduo.display.ScreenRenderer;
import io.github.mmilk23.screenduo.flight.FlightRoute;
import java.text.Normalizer;
import java.util.Locale;

public final class AircraftDetailScreenRenderer implements ScreenRenderer<AircraftDetailScreenData> {

    private static final DisplayGeometry GEOMETRY = new DisplayGeometry(320, 240);
    private static final RgbColor BACKGROUND = new RgbColor(5, 12, 28);
    private static final RgbColor PRIMARY = new RgbColor(238, 246, 255);
    private static final RgbColor SECONDARY = new RgbColor(121, 175, 213);
    private static final RgbColor ACCENT = new RgbColor(53, 200, 255);
    private static final int ROUTE_X = 12;
    private static final int ROUTE_WIDTH = 296;

    @Override
    public RgbFrame render(DisplayGeometry geometry, AircraftDetailScreenData data) {
        NearbyAircraft aircraft = data.aircraft();
        RgbFrame frame = new RgbFrame(GEOMETRY);
        FrameCanvas canvas = new FrameCanvas(frame);
        canvas.clear(BACKGROUND);
        canvas.fillRectangle(0, 0, 320, 34, new RgbColor(12, 39, 70));
        canvas.drawText(AircraftScreenText.fit(AircraftScreenText.flight(aircraft), 16),
                12, 8, 3, PRIMARY);
        data.airlineLogo().ifPresent(logo -> {
            canvas.fillRectangle(274, 5, 36, 24, PRIMARY);
            canvas.drawFrame(logo, 276, 5, 1);
        });
        canvas.drawText(AircraftScreenText.fit(AircraftScreenText.airline(aircraft), 49),
                12, 43, 1, ACCENT);
        field(canvas, "DISTANCE", AircraftScreenText.number(aircraft.distanceKm(), "%.1f KM"), 65);
        field(canvas, "BEARING FROM YOU", AircraftScreenText.bearing(aircraft.bearingDegrees()), 94);
        field(canvas, "ALTITUDE", AircraftScreenText.number(aircraft.altitudeMeters(), "%.0f M"), 123);
        field(canvas, "SPEED", AircraftScreenText.number(aircraft.speedKilometersPerHour(), "%.0f KM/H"), 152);
        canvas.drawText(aircraft.onGround() ? "ON GROUND" : "AIRBORNE", 12, 55, 1, ACCENT);
        if (data.route().isPresent()) {
            FlightRoute route = data.route().orElseThrow();
            RouteText routeText = routeText(route);
            canvas.drawText("PLANNED ROUTE", ROUTE_X, 180, 1, SECONDARY);
            int routeY = routeText.scale() == 2 ? 197 : 203;
            CompactPixelText.draw(canvas, routeText.text(), ROUTE_X, routeY, routeText.scale(), PRIMARY);
        } else {
            canvas.drawText("ROUTE UNAVAILABLE", 12, 193, 1, SECONDARY);
        }
        canvas.drawText(AircraftScreenText.snapshot(data.loadedAt()), 12, 225, 1, SECONDARY);
        canvas.drawText("BACK LIST", 254, 225, 1, SECONDARY);
        return GEOMETRY.equals(geometry) ? frame : FrameScaler.fit(frame, geometry, BACKGROUND);
    }

    static RouteText routeText(FlightRoute route) {
        String cityRoute = normalize(route.originDisplayName() + " > " + route.destinationDisplayName());
        if (pixelWidth(cityRoute, 1) <= ROUTE_WIDTH) {
            return new RouteText(cityRoute, pixelWidth(cityRoute, 2) <= ROUTE_WIDTH ? 2 : 1);
        }
        String icaoRoute = route.originIcao() + " > " + route.destinationIcao();
        return new RouteText(icaoRoute, pixelWidth(icaoRoute, 2) <= ROUTE_WIDTH ? 2 : 1);
    }

    record RouteText(String text, int scale) {
    }

    private static int pixelWidth(String text, int scale) {
        return CompactPixelText.width(text, scale);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
    }

    private static void field(FrameCanvas canvas, String label, String value, int y) {
        canvas.drawText(label, 12, y + 3, 1, SECONDARY);
        canvas.drawText(AircraftScreenText.fit(value, 16), 116, y, 2, PRIMARY);
    }
}
