package io.github.mmilk23.screenduo.aircraft.display;

import io.github.mmilk23.screenduo.aircraft.NearbyAircraft;
import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.FrameCanvas;
import io.github.mmilk23.screenduo.display.FrameScaler;
import io.github.mmilk23.screenduo.display.RgbColor;
import io.github.mmilk23.screenduo.display.RgbFrame;
import io.github.mmilk23.screenduo.display.ScreenRenderer;

public final class AircraftListScreenRenderer implements ScreenRenderer<AircraftListScreenData> {

    public static final int PAGE_SIZE = 4;
    private static final DisplayGeometry GEOMETRY = new DisplayGeometry(320, 240);
    private static final RgbColor BACKGROUND = new RgbColor(5, 12, 28);
    private static final RgbColor PRIMARY = new RgbColor(238, 246, 255);
    private static final RgbColor SECONDARY = new RgbColor(121, 175, 213);
    private static final RgbColor ACCENT = new RgbColor(53, 200, 255);

    @Override
    public RgbFrame render(DisplayGeometry geometry, AircraftListScreenData data) {
        RgbFrame frame = new RgbFrame(GEOMETRY);
        FrameCanvas canvas = canvas(frame);
        canvas.drawText(AircraftScreenText.snapshot(data.loadedAt()), 12, 39, 1, SECONDARY);
        if (data.aircraft().isEmpty()) {
            canvas.drawText("NO AIRCRAFT", 40, 100, 3, ACCENT);
        } else {
            int pageStart = data.selectedIndex() / PAGE_SIZE * PAGE_SIZE;
            String page = (pageStart / PAGE_SIZE + 1) + "/"
                    + ((data.aircraft().size() + PAGE_SIZE - 1) / PAGE_SIZE);
            canvas.drawText(page, 308 - canvas.textWidth(page, 1), 13, 1, ACCENT);
            for (int index = pageStart;
                    index < Math.min(pageStart + PAGE_SIZE, data.aircraft().size()); index++) {
                NearbyAircraft aircraft = data.aircraft().get(index);
                drawRow(canvas, aircraft,
                        data.airlineLogos().get(aircraft.airlineIcaoCode()),
                        53 + (index - pageStart) * 40, index == data.selectedIndex());
            }
        }
        canvas.drawText("UP DOWN", 12, 225, 1, SECONDARY);
        canvas.drawText("OK DETAILS", 122, 225, 1, SECONDARY);
        canvas.drawText("BACK EXIT", 254, 225, 1, SECONDARY);
        return fit(frame, geometry);
    }

    public RgbFrame renderError(DisplayGeometry geometry) {
        RgbFrame frame = new RgbFrame(GEOMETRY);
        FrameCanvas canvas = canvas(frame);
        canvas.drawText("AIRCRAFT DATA ERROR", 46, 93, 2, ACCENT);
        canvas.drawText("CHECK CONNECTION OR API LIMIT", 73, 121, 1, SECONDARY);
        canvas.drawText("OK RETRY", 12, 225, 1, SECONDARY);
        canvas.drawText("BACK EXIT", 254, 225, 1, SECONDARY);
        return fit(frame, geometry);
    }

    private static FrameCanvas canvas(RgbFrame frame) {
        FrameCanvas canvas = new FrameCanvas(frame);
        canvas.clear(BACKGROUND);
        canvas.fillRectangle(0, 0, 320, 34, new RgbColor(12, 39, 70));
        canvas.drawText("AIRCRAFT", 12, 8, 3, PRIMARY);
        return canvas;
    }

    private static void drawRow(
            FrameCanvas canvas, NearbyAircraft aircraft, RgbFrame logo, int y, boolean selected) {
        RgbColor text = selected ? new RgbColor(3, 24, 38) : PRIMARY;
        RgbColor detail = selected ? text : SECONDARY;
        canvas.fillRectangle(8, y, 304, 36,
                selected ? ACCENT : new RgbColor(11, 28, 48));
        int textX = 16;
        if (logo != null) {
            canvas.fillRectangle(13, y + 6, 36, 24, selected ? new RgbColor(225, 245, 252) : PRIMARY);
            canvas.drawFrame(logo, 15, y + 6, 1);
            textX = 56;
        }
        canvas.drawText(AircraftScreenText.fit(AircraftScreenText.flight(aircraft), logo == null ? 12 : 8),
                textX, y + 4, 2, text);
        String distance = AircraftScreenText.number(aircraft.distanceKm(), "%.1f KM");
        canvas.drawText(distance, 304 - canvas.textWidth(distance, 1), y + 5, 1, detail);
        canvas.drawText(AircraftScreenText.fit(AircraftScreenText.airline(aircraft), logo == null ? 47 : 40),
                textX, y + 23, 1, detail);
    }

    private static RgbFrame fit(RgbFrame frame, DisplayGeometry geometry) {
        return GEOMETRY.equals(geometry) ? frame : FrameScaler.fit(frame, geometry, BACKGROUND);
    }
}