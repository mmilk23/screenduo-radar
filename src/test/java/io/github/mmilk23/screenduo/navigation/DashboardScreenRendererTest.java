package io.github.mmilk23.screenduo.navigation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.RgbFrame;
import org.junit.jupiter.api.Test;

class DashboardScreenRendererTest {

    @Test
    void addsControlsInPlaceForLogicalGeometry() {
        DashboardScreenRenderer renderer = new DashboardScreenRenderer();
        RgbFrame frame = new RgbFrame(DashboardScreenRenderer.LOGICAL_GEOMETRY);

        RgbFrame rendered = renderer.withControls(frame, DashboardScreenRenderer.LOGICAL_GEOMETRY, "UP/DOWN SELECT");

        assertSame(frame, rendered);
        assertEquals(5, rendered.redAt(0, 216));
        assertEquals(12, rendered.greenAt(0, 216));
        assertEquals(28, rendered.blueAt(0, 216));
    }

    @Test
    void scalesDashboardWhenPhysicalGeometryDiffers() {
        DashboardScreenRenderer renderer = new DashboardScreenRenderer();
        RgbFrame frame = new RgbFrame(DashboardScreenRenderer.LOGICAL_GEOMETRY);

        RgbFrame rendered = renderer.withControls(frame, new DisplayGeometry(640, 480), "BACK RETURN");

        assertEquals(new DisplayGeometry(640, 480), rendered.geometry());
    }

    @Test
    void rendersErrorScreen() {
        DashboardScreenRenderer renderer = new DashboardScreenRenderer();

        RgbFrame frame = renderer.error("NETWORK ERROR");

        assertEquals(DashboardScreenRenderer.LOGICAL_GEOMETRY, frame.geometry());
        assertEquals(5, frame.redAt(0, 0));
        assertEquals(12, frame.greenAt(0, 0));
        assertEquals(28, frame.blueAt(0, 0));
    }
}
