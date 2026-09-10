package io.github.mmilk23.screenduo.device;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mmilk23.screenduo.display.DisplayButton;
import org.junit.jupiter.api.Test;

class ScreenDuoButtonMapperTest {

    @Test
    void mapsAllKnownButtons() {
        assertEquals(DisplayButton.CONFIRM, ScreenDuoButtonMapper.map(0).button());
        assertEquals(DisplayButton.LEFT, ScreenDuoButtonMapper.map(1).button());
        assertEquals(DisplayButton.RIGHT, ScreenDuoButtonMapper.map(2).button());
        assertEquals(DisplayButton.UP, ScreenDuoButtonMapper.map(3).button());
        assertEquals(DisplayButton.DOWN, ScreenDuoButtonMapper.map(4).button());
        assertEquals(DisplayButton.BACK, ScreenDuoButtonMapper.map(6).button());
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
