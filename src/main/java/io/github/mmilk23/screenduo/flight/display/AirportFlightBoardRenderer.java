package io.github.mmilk23.screenduo.flight.display;

import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.FrameCanvas;
import io.github.mmilk23.screenduo.display.FrameScaler;
import io.github.mmilk23.screenduo.display.RgbColor;
import io.github.mmilk23.screenduo.display.RgbFrame;
import io.github.mmilk23.screenduo.display.ScreenRenderer;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class AirportFlightBoardRenderer implements ScreenRenderer<AirportFlightBoardData> {
    public static final int PAGE_SIZE = 5;
    private static final DisplayGeometry GEOMETRY = new DisplayGeometry(320, 240);
    private static final RgbColor BACKGROUND = new RgbColor(5, 12, 28);
    private static final RgbColor PRIMARY = new RgbColor(238, 246, 255);
    private static final RgbColor SECONDARY = new RgbColor(121, 175, 213);
    private static final RgbColor ACCENT = new RgbColor(53, 200, 255);
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT).withZone(ZoneOffset.UTC);

    @Override
    public RgbFrame render(DisplayGeometry geometry, AirportFlightBoardData data) {
        RgbFrame frame = new RgbFrame(GEOMETRY);
        FrameCanvas canvas = new FrameCanvas(frame);
        canvas.clear(BACKGROUND);
        canvas.fillRectangle(0, 0, 320, 34, new RgbColor(12, 39, 70));
        canvas.drawText(data.airportIcao() + " " + data.direction().name(), 12, 10, 2, PRIMARY);
        canvas.drawText(data.date() + " UTC  PLANNED", 12, 41, 1, SECONDARY);
        int pageStart = data.selectedIndex() / PAGE_SIZE * PAGE_SIZE;
        if (data.flights().isEmpty()) {
            canvas.drawText("NO SCHEDULED FLIGHTS", 46, 100, 2, ACCENT);
            canvas.drawText("SIROS COVERAGE: BRAZIL", 97, 128, 1, SECONDARY);
        } else {
            for (int index = pageStart;
                    index < Math.min(pageStart + PAGE_SIZE, data.flights().size()); index++) {
                var flight = data.flights().get(index);
                int y = 57 + (index - pageStart) * 29;
                boolean selected = index == data.selectedIndex();
                RgbColor text = selected ? new RgbColor(3, 24, 38) : PRIMARY;
                canvas.fillRectangle(8, y, 304, 25,
                        selected ? ACCENT : new RgbColor(11, 28, 48));
                canvas.drawText(TIME.format(AirportFlightBoardData.eventTime(flight, data.direction())),
                        15, y + 6, 2, text);
                canvas.drawText(flight.callsign(), 88, y + 6, 2, text);
                String otherAirport = data.direction() == AirportFlightBoardData.Direction.ARRIVALS
                        ? flight.route().originDisplayName() : flight.route().destinationDisplayName();
                canvas.drawText(fit(otherAirport, 16), 210, y + 9, 1, text);
            }
        }
        String page = data.flights().isEmpty() ? "0/0"
                : (pageStart / PAGE_SIZE + 1) + "/" + ((data.flights().size() + PAGE_SIZE - 1) / PAGE_SIZE);
        canvas.drawText("SIROS  " + page, 12, 205, 1, SECONDARY);
        canvas.drawText("LEFT ARR  RIGHT DEP  BACK RETURN", 12, 225, 1, SECONDARY);
        return GEOMETRY.equals(geometry) ? frame : FrameScaler.fit(frame, geometry, BACKGROUND);
    }
    private static String fit(String value, int maximumLength) {
        String text = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toUpperCase(Locale.ROOT);
        return text.length() <= maximumLength ? text : text.substring(0, maximumLength - 3) + "...";
    }
}
