package io.github.mmilk23.screenduo.device;

import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.RgbFrame;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

final class ScreenDuoProtocol {

    static final DisplayGeometry GEOMETRY = new DisplayGeometry(320, 240);
    static final int IMAGE_HEADER_SIZE = 32;
    static final int MAX_BLOCK_SIZE = 0x10000;
    static final int COMMAND_SIZE = 31;
    static final int STATUS_SIZE = 13;
    static final int FOOTER_SIZE = 512;

    private static final int COMMAND_SIGNATURE = 0x43425355;
    private static final int IMAGE_TAG = 0x843d84a0;
    private static final short IMAGE_COMMAND = (short) 0x02e6;

    private ScreenDuoProtocol() {
    }

    static byte[] encodeFrame(RgbFrame frame) {
        if (!GEOMETRY.equals(frame.geometry())) {
            throw new IllegalArgumentException(
                    "ScreenDUO requires a 320x240 frame, received "
                            + frame.geometry().width() + "x" + frame.geometry().height());
        }

        byte[] pixels = frame.pixels();
        ByteBuffer encoded = ByteBuffer
                .allocate(IMAGE_HEADER_SIZE + pixels.length)
                .order(ByteOrder.LITTLE_ENDIAN);

        encoded.put((byte) 0x02);
        encoded.put((byte) 0xf0);
        encoded.putShort((short) 0x0020);
        encoded.putInt(encoded.capacity());
        encoded.putShort((short) 0);
        encoded.putShort((short) 0);
        encoded.putShort((short) GEOMETRY.width());
        encoded.putShort((short) GEOMETRY.height());
        encoded.putShort((short) 0);
        encoded.putShort((short) 1);
        for (int index = 0; index < 6; index++) {
            encoded.putShort((short) 0);
        }
        encoded.put(pixels);
        return encoded.array();
    }

    static byte[] imageBlockCommand(int blockLength, int totalLength, int blockIndex) {
        validateBlock(blockLength, totalLength, blockIndex);
        ByteBuffer command = commandPrefix(blockLength);
        command.putShort(IMAGE_COMMAND);
        putNetworkOrderInt(command, blockLength);
        putNetworkOrderInt(command, totalLength);
        command.put((byte) blockIndex);
        command.put((byte) 0);
        command.putInt(0);
        return command.array();
    }

    static byte[] imageBlockFooter(int blockLength, int totalLength, int blockIndex) {
        validateBlock(blockLength, totalLength, blockIndex);
        ByteBuffer footer = ByteBuffer.allocate(FOOTER_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        putCommandPrefix(footer, blockLength);
        footer.putShort(IMAGE_COMMAND);
        putNetworkOrderInt(footer, blockLength);
        putNetworkOrderInt(footer, totalLength);
        footer.put((byte) blockIndex);
        footer.put((byte) 0);
        return footer.array();
    }

    private static ByteBuffer commandPrefix(int blockLength) {
        ByteBuffer command = ByteBuffer.allocate(COMMAND_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        putCommandPrefix(command, blockLength);
        return command;
    }

    private static void putCommandPrefix(ByteBuffer target, int blockLength) {
        target.putInt(COMMAND_SIGNATURE);
        target.putInt(IMAGE_TAG);
        target.putInt(blockLength);
        target.put((byte) 0);
        target.put((byte) 0);
        target.put((byte) 0x0c);
    }

    private static void putNetworkOrderInt(ByteBuffer target, int value) {
        target.order(ByteOrder.BIG_ENDIAN).putInt(value).order(ByteOrder.LITTLE_ENDIAN);
    }

    private static void validateBlock(int blockLength, int totalLength, int blockIndex) {
        if (blockLength <= 0 || blockLength > MAX_BLOCK_SIZE) {
            throw new IllegalArgumentException("Invalid ScreenDUO block length: " + blockLength);
        }
        if (totalLength < blockLength) {
            throw new IllegalArgumentException("Total length cannot be smaller than block length");
        }
        if (blockIndex < 0 || blockIndex > 255) {
            throw new IllegalArgumentException("Invalid ScreenDUO block index: " + blockIndex);
        }
    }
}
