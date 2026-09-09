package io.github.mmilk23.screenduo.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class StatusScreenRendererTest {

    @Test
    void rendersLogicalGeometryAndNormalizesLongText() {
        StatusScreenRenderer renderer = new StatusScreenRenderer();

        RgbFrame frame = renderer.render(
                new DisplayGeometry(320, 240),
                "Conexão estabelecida com título longo",
                "Atualização meteorológica extraordinariamente demorada no aeroporto selecionado");

        assertEquals(new DisplayGeometry(320, 240), frame.geometry());
        assertFalse(allBlack(frame));
        assertEquals(12, frame.redAt(0, 0));
        assertEquals(39, frame.greenAt(0, 0));
        assertEquals(70, frame.blueAt(0, 0));
    }

    @Test
    void scalesStatusScreenToTargetGeometry() {
        StatusScreenRenderer renderer = new StatusScreenRenderer();

        RgbFrame frame = renderer.render(new DisplayGeometry(160, 120), "Loading", "Weather");

        assertEquals(new DisplayGeometry(160, 120), frame.geometry());
        assertFalse(allBlack(frame));
    }

    private static boolean allBlack(RgbFrame frame) {
        for (byte value : frame.pixels()) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }
}
