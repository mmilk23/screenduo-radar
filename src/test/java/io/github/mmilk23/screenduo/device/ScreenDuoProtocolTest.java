package io.github.mmilk23.screenduo.device;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.mmilk23.screenduo.display.RgbFrame;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import org.junit.jupiter.api.Test;

class ScreenDuoProtocolTest {

    @Test
    void encodesFrameHeaderAndPixels() {
        RgbFrame frame = new RgbFrame(ScreenDuoProtocol.GEOMETRY);
        frame.setPixel(0, 0, 255, 128, 1);

        byte[] encoded = ScreenDuoProtocol.encodeFrame(frame);
        ByteBuffer header = ByteBuffer.wrap(encoded).order(ByteOrder.LITTLE_ENDIAN);

        assertEquals(230432, encoded.length);
        assertEquals(0x02, Byte.toUnsignedInt(header.get()));
        assertEquals(0xf0, Byte.toUnsignedInt(header.get()));
        assertEquals(0x20, Short.toUnsignedInt(header.getShort()));
        assertEquals(encoded.length, header.getInt());
        assertEquals(320, Short.toUnsignedInt(header.getShort(12)));
        assertEquals(240, Short.toUnsignedInt(header.getShort(14)));
        assertArrayEquals(
                new byte[] {(byte) 255, (byte) 128, 1},
                new byte[] {encoded[32], encoded[33], encoded[34]});
    }

    @Test
    void createsMassStorageStyleBlockCommand() {
        byte[] command = ScreenDuoProtocol.imageBlockCommand(0x10000, 230432, 2);

        assertEquals(31, command.length);
        assertArrayEquals(
                new byte[] {0x55, 0x53, 0x42, 0x43},
                new byte[] {command[0], command[1], command[2], command[3]});
        assertArrayEquals(
                new byte[] {0x00, 0x01, 0x00, 0x00},
                new byte[] {command[17], command[18], command[19], command[20]});
        assertEquals(2, Byte.toUnsignedInt(command[25]));
    }

    @Test
    void createsFixedSizeFooter() {
        assertEquals(
                512,
                ScreenDuoProtocol.imageBlockFooter(1024, 230432, 0).length);
    }
}
