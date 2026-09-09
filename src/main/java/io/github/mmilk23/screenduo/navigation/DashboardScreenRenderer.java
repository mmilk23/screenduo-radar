package io.github.mmilk23.screenduo.navigation;

import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.FrameCanvas;
import io.github.mmilk23.screenduo.display.FrameScaler;
import io.github.mmilk23.screenduo.display.RgbColor;
import io.github.mmilk23.screenduo.display.RgbFrame;

public final class DashboardScreenRenderer {
    public static final DisplayGeometry LOGICAL_GEOMETRY = new DisplayGeometry(320, 240);
    private static final RgbColor BACKGROUND = new RgbColor(5, 12, 28);
    private static final RgbColor TEXT = new RgbColor(121, 175, 213);

    public RgbFrame withControls(RgbFrame logicalFrame, DisplayGeometry geometry, String controls) {
        FrameCanvas canvas = new FrameCanvas(logicalFrame);
        canvas.fillRectangle(0, 216, 320, 24, BACKGROUND);
        canvas.drawText(controls, 8, 218, 1, TEXT);
        canvas.drawText("1 WEATHER   2 AIRPORTS", 8, 231, 1, TEXT);
        return LOGICAL_GEOMETRY.equals(geometry)
                ? logicalFrame : FrameScaler.fit(logicalFrame, geometry, BACKGROUND);
    }

    public RgbFrame error(String message) {
        RgbFrame frame = new RgbFrame(LOGICAL_GEOMETRY);
        FrameCanvas canvas = new FrameCanvas(frame);
        canvas.clear(BACKGROUND);
        canvas.drawText("DATA UNAVAILABLE", 16, 45, 3, new RgbColor(238, 246, 255));
        canvas.drawText(message, 16, 105, 2, new RgbColor(53, 200, 255));
        return frame;
    }
}