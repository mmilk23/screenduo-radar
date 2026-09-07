package io.github.mmilk23.screenduo.device;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ScreenDuoDeviceTest {

    @Test
    void matchesScreenDuoUsbIdentifier() {
        assertTrue(ScreenDuoDevice.matches((short) 0x1043, (short) 0x3100));
    }

    @Test
    void rejectsOtherUsbIdentifiers() {
        assertFalse(ScreenDuoDevice.matches((short) 0x1043, (short) 0x3101));
        assertFalse(ScreenDuoDevice.matches((short) 0x0000, (short) 0x3100));
    }
}
