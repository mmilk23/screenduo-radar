package io.github.mmilk23.screenduo.airport.display;

import io.github.mmilk23.screenduo.airport.NearbyAirport;
import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.FrameCanvas;
import io.github.mmilk23.screenduo.display.FrameScaler;
import io.github.mmilk23.screenduo.display.RgbColor;
import io.github.mmilk23.screenduo.display.RgbFrame;
import io.github.mmilk23.screenduo.display.ScreenRenderer;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

public final class AirportListScreenRenderer implements ScreenRenderer<AirportListScreenData> {

    public static final int PAGE_SIZE = 5;
    private static final DisplayGeometry DASHBOARD_GEOMETRY = new DisplayGeometry(320, 240);
    private static final RgbColor BACKGROUND = new RgbColor(5, 12, 28);
    private static final RgbColor HEADER = new RgbColor(12, 39, 70);
    private static final RgbColor CARD = new RgbColor(11, 28, 48);
    private static final RgbColor PRIMARY = new RgbColor(238, 246, 255);
    private static final RgbColor SECONDARY = new RgbColor(121, 175, 213);
    private static final RgbColor ACCENT = new RgbColor(53, 200, 255);
    private static final RgbColor SELECTED_TEXT = new RgbColor(3, 24, 38);

    @Override
    public RgbFrame render(DisplayGeometry geometry, AirportListScreenData data) {
        RgbFrame dashboard = renderDashboard(data);
        if (DASHBOARD_GEOMETRY.equals(geometry)) {
            return dashboard;
        }
        return FrameScaler.fit(dashboard, geometry, BACKGROUND);
    }

    private static RgbFrame renderDashboard(AirportListScreenData data) {
        RgbFrame frame = new RgbFrame(DASHBOARD_GEOMETRY);
        FrameCanvas canvas = new FrameCanvas(frame);
        canvas.clear(BACKGROUND);
        canvas.fillRectangle(0, 0, DASHBOARD_GEOMETRY.width(), 34, HEADER);
        canvas.drawText("AIRPORTS", 12, 8, 3, PRIMARY);

        if (data.airports().isEmpty()) {
            canvas.drawText("NO AIRPORTS", 40, 92, 3, ACCENT);
            canvas.drawText("BACK TO EXIT", 106, 225, 1, SECONDARY);
            return frame;
        }

        int pageStart = data.selectedIndex() / PAGE_SIZE * PAGE_SIZE;
        int pageCount = (data.airports().size() + PAGE_SIZE - 1) / PAGE_SIZE;
        int currentPage = pageStart / PAGE_SIZE + 1;
        canvas.drawText(currentPage + "/" + pageCount, 286, 13, 1, ACCENT);

        List<NearbyAirport> visible = data.airports().stream()
                .skip(pageStart)
                .limit(PAGE_SIZE)
                .toList();
        for (int index = 0; index < visible.size(); index++) {
            int absoluteIndex = pageStart + index;
            drawAirportRow(
                    canvas,
                    visible.get(index),
                    39 + index * 36,
                    absoluteIndex == data.selectedIndex());
        }

        canvas.drawText("UP DOWN", 12, 225, 1, SECONDARY);
        canvas.drawText("OK SELECT", 134, 225, 1, SECONDARY);
        canvas.drawText("BACK EXIT", 255, 225, 1, SECONDARY);
        return frame;
    }

    private static void drawAirportRow(
            FrameCanvas canvas,
            NearbyAirport airport,
            int y,
            boolean selected) {
        RgbColor background = selected ? ACCENT : CARD;
        RgbColor mainText = selected ? SELECTED_TEXT : PRIMARY;
        RgbColor detailText = selected ? SELECTED_TEXT : SECONDARY;
        canvas.fillRectangle(8, y, 304, 32, background);
        if (selected) {
            canvas.fillRectangle(8, y, 4, 32, PRIMARY);
        }

        canvas.drawText(code(airport), 17, y + 6, 2, mainText);
        canvas.drawText(fit(normalize(airport.name()), 34), 82, y + 4, 1, mainText);
        canvas.drawText(
                String.format(Locale.ROOT, "%.1f KM  %s",
                        airport.distanceKm(), compass(airport.bearingDegrees())),
                82,
                y + 18,
                1,
                detailText);
    }

    private static String code(NearbyAirport airport) {
        String value = airport.iataCode().isBlank() ? airport.ident() : airport.iataCode();
        return fit(normalize(value), 4);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
    }

    private static String fit(String value, int maximumLength) {
        return value.length() <= maximumLength
                ? value
                : value.substring(0, maximumLength - 3) + "...";
    }

    private static String compass(double degrees) {
        String[] directions = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int index = (int) Math.round((degrees % 360.0) / 45.0) % directions.length;
        return directions[index];
    }
}
