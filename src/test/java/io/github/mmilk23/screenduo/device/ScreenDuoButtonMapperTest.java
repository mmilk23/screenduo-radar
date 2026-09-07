package io.github.mmilk23.screenduo.device;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mmilk23.screenduo.display.DisplayButton;
import org.junit.jupiter.api.Test;

class ScreenDuoButtonMapperTest {

    @Test
    void mapsKnownApplicationButtons() {
        assertEquals(DisplayButton.ACTION_1, ScreenDuoButtonMapper.map(12).button());
        assertEquals(DisplayButton.ACTION_2, ScreenDuoButtonMapper.map(13).button());
    }

    @Test
    void preservesUnknownNativeCode() {
        var event = ScreenDuoButtonMapper.map(99);

        assertEquals(DisplayButton.UNKNOWN, event.button());
        assertEquals(99, event.sourceCode());
    }
}
