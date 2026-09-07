package io.github.mmilk23.screenduo.display;

import java.text.Normalizer;
import java.util.Locale;

public final class StatusScreenRenderer {

    private static final DisplayGeometry DASHBOARD_GEOMETRY = new DisplayGeometry(320, 240);
    private static final RgbColor BACKGROUND = new RgbColor(5, 12, 28);
    private static final RgbColor PRIMARY = new RgbColor(238, 246, 255);
    private static final RgbColor SECONDARY = new RgbColor(121, 175, 213);
    private static final RgbColor ACCENT = new RgbColor(53, 200, 255);

    public RgbFrame render(DisplayGeometry geometry, String title, String message) {
        RgbFrame dashboard = new RgbFrame(DASHBOARD_GEOMETRY);
        FrameCanvas canvas = new FrameCanvas(dashboard);
        canvas.clear(BACKGROUND);
        canvas.fillRectangle(0, 0, DASHBOARD_GEOMETRY.width(), 34, new RgbColor(12, 39, 70));
        canvas.drawText(fit(normalize(title), 16), 12, 8, 3, PRIMARY);
        canvas.drawText(fit(normalize(message), 23), 22, 100, 2, ACCENT);
        canvas.drawText("PLEASE WAIT", 125, 130, 1, SECONDARY);

        return DASHBOARD_GEOMETRY.equals(geometry)
                ? dashboard
                : FrameScaler.fit(dashboard, geometry, BACKGROUND);
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
}
