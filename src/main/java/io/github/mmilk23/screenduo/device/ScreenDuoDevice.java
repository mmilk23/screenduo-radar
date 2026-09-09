package io.github.mmilk23.screenduo.device;

import io.github.mmilk23.screenduo.display.Display;
import io.github.mmilk23.screenduo.display.DisplayButtonEvent;
import io.github.mmilk23.screenduo.display.DisplayControls;
import io.github.mmilk23.screenduo.display.DisplayGeometry;
import io.github.mmilk23.screenduo.display.RgbFrame;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.usb4java.BufferUtils;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

public final class ScreenDuoDevice implements Display, DisplayControls {

    public static final short VENDOR_ID = (short) 0x1043;
    public static final short PRODUCT_ID = (short) 0x3100;

    private static final int INTERFACE_NUMBER = 0;
    private static final int ALTERNATE_SETTING = 0;
    private static final byte WRITE_ENDPOINT = (byte) 0x02;
    private static final byte READ_ENDPOINT = (byte) 0x81;

    private static final long COMMAND_TIMEOUT_MILLIS = 1_000;
    private static final long DATA_TIMEOUT_MILLIS = 5_000;
    private static final long STATUS_TIMEOUT_MILLIS = 2_000;
    private static final long BUTTON_TIMEOUT_MILLIS = 500;
    private static final long BUTTON_DRAIN_TIMEOUT_MILLIS = 100;
    private static final long IMAGE_SYNC_TIMEOUT_MILLIS = 25;
    private static final int MAX_BUTTON_DRAIN_READS = 8;
    private static final int MAX_IMAGE_SYNC_READS = 8;

    private final Context context;
    private final DeviceHandle handle;
    private final String descriptorReport;
    private boolean closed;

    private ScreenDuoDevice(Context context, DeviceHandle handle, String descriptorReport) {
        this.context = context;
        this.handle = handle;
        this.descriptorReport = descriptorReport;
    }

    public static Optional<ScreenDuoDevice> open() {
        Context context = new Context();
        int result = LibUsb.init(context);
        ensureSuccess(result, "Unable to initialize libusb");

        try {
            Optional<OpenedDevice> detectedDevice = findAndOpen(context);
            if (detectedDevice.isPresent()) {
                OpenedDevice openedDevice = detectedDevice.orElseThrow();
                return Optional.of(new ScreenDuoDevice(
                        context, openedDevice.handle(), openedDevice.descriptorReport()));
            }
        } catch (RuntimeException exception) {
            LibUsb.exit(context);
            throw exception;
        }

        LibUsb.exit(context);
        return Optional.empty();
    }

    public static boolean matches(short vendorId, short productId) {
        return vendorId == VENDOR_ID && productId == PRODUCT_ID;
    }

    public String descriptorReport() {
        return descriptorReport;
    }

    @Override
    public DisplayGeometry geometry() {
        return ScreenDuoProtocol.GEOMETRY;
    }

    @Override
    public void show(RgbFrame frame) {
        ensureOpen();
        synchronizeBeforeImageTransfer();
        byte[] encodedFrame = ScreenDuoProtocol.encodeFrame(frame);

        int position = 0;
        int blockIndex = 0;
        while (position < encodedFrame.length) {
            int blockLength = Math.min(
                    ScreenDuoProtocol.MAX_BLOCK_SIZE, encodedFrame.length - position);

            writeExact(
                    ScreenDuoProtocol.imageBlockCommand(
                            blockLength, encodedFrame.length, blockIndex),
                    COMMAND_TIMEOUT_MILLIS,
                    "Unable to write ScreenDUO block command");

            writeExact(
                    encodedFrame,
                    position,
                    blockLength,
                    DATA_TIMEOUT_MILLIS,
                    "Unable to write ScreenDUO image block");

            readStatus();

            writeExact(
                    ScreenDuoProtocol.imageBlockFooter(
                            blockLength, encodedFrame.length, blockIndex),
                    COMMAND_TIMEOUT_MILLIS,
                    "Unable to write ScreenDUO block footer");

            position += blockLength;
            blockIndex++;
        }
    }

    @Override
    public Optional<DisplayButtonEvent> pollButton() {
        return probeButton().event();
    }

    public ScreenDuoButtonProbe probeButton() {
        ensureOpen();
        writeExact(
                ScreenDuoProtocol.buttonPollCommand(),
                COMMAND_TIMEOUT_MILLIS,
                "Unable to request ScreenDUO button state");

        UsbReadResult response = readUsb(
                ScreenDuoProtocol.BUTTON_RESPONSE_SIZE,
                BUTTON_TIMEOUT_MILLIS,
                "Unable to read ScreenDUO button response");

        Optional<DisplayButtonEvent> event = decodeButtonEvent(response.data());
        List<byte[]> drainedPackets = new ArrayList<>();
        UsbReadResult lastRead = response;

        for (int attempt = 0; attempt < MAX_BUTTON_DRAIN_READS; attempt++) {
            UsbReadResult drained = readUsb(
                    ScreenDuoProtocol.FOOTER_SIZE,
                    BUTTON_DRAIN_TIMEOUT_MILLIS,
                    "Unable to drain ScreenDUO button response");
            lastRead = drained;
            if (drained.data().length == 0) {
                break;
            }
            drainedPackets.add(drained.data());
            if (event.isEmpty()) {
                event = decodeButtonEvent(drained.data());
            }
        }

        int clearHaltStatus = LibUsb.clearHalt(handle, READ_ENDPOINT);
        ensureSuccess(clearHaltStatus, "Unable to clear ScreenDUO button endpoint");

        UsbReadResult finalRead = readUsb(
                ScreenDuoProtocol.FOOTER_SIZE,
                BUTTON_DRAIN_TIMEOUT_MILLIS,
                "Unable to finish ScreenDUO button transaction");
        lastRead = finalRead;
        if (finalRead.data().length > 0) {
            drainedPackets.add(finalRead.data());
            if (event.isEmpty()) {
                event = decodeButtonEvent(finalRead.data());
            }
        }

        return new ScreenDuoButtonProbe(
                response.status(),
                response.data(),
                clearHaltStatus,
                lastRead.status(),
                concatenate(drainedPackets),
                event);
    }

    private void synchronizeBeforeImageTransfer() {
        for (int attempt = 0; attempt < MAX_IMAGE_SYNC_READS; attempt++) {
            UsbReadResult stale = readUsb(
                    ScreenDuoProtocol.FOOTER_SIZE,
                    IMAGE_SYNC_TIMEOUT_MILLIS,
                    "Unable to synchronize ScreenDUO read endpoint");
            if (stale.data().length == 0) {
                break;
            }
        }

        int result = LibUsb.clearHalt(handle, READ_ENDPOINT);
        ensureSuccess(result, "Unable to synchronize ScreenDUO read endpoint");
        result = LibUsb.clearHalt(handle, WRITE_ENDPOINT);
        ensureSuccess(result, "Unable to synchronize ScreenDUO write endpoint");
    }

    private Optional<DisplayButtonEvent> decodeButtonEvent(byte[] data) {
        return ScreenDuoProtocol.decodeLastButtonCode(data)
                .stream()
                .mapToObj(ScreenDuoButtonMapper::map)
                .findFirst();
    }

    private static byte[] concatenate(List<byte[]> packets) {
        int length = packets.stream().mapToInt(packet -> packet.length).sum();
        byte[] result = new byte[length];
        int offset = 0;
        for (byte[] packet : packets) {
            System.arraycopy(packet, 0, result, offset, packet.length);
            offset += packet.length;
        }
        return result;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        LibUsb.releaseInterface(handle, INTERFACE_NUMBER);
        LibUsb.close(handle);
        LibUsb.exit(context);
        closed = true;
    }

    private void writeExact(byte[] data, long timeout, String message) {
        writeExact(data, 0, data.length, timeout, message);
    }

    private void writeExact(byte[] data, int offset, int length, long timeout, String message) {
        ByteBuffer buffer = BufferUtils.allocateByteBuffer(length);
        buffer.put(data, offset, length);
        buffer.rewind();

        IntBuffer transferred = BufferUtils.allocateIntBuffer();
        int result = LibUsb.bulkTransfer(handle, WRITE_ENDPOINT, buffer, transferred, timeout);
        ensureSuccess(result, message);

        int transferredBytes = transferred.get(0);
        if (transferredBytes != length) {
            throw new IllegalStateException(
                    message + ": expected " + length + " bytes, wrote " + transferredBytes);
        }
    }

    private UsbReadResult readUsb(int capacity, long timeout, String message) {
        ByteBuffer buffer = BufferUtils.allocateByteBuffer(capacity);
        IntBuffer transferred = BufferUtils.allocateIntBuffer();
        int result = LibUsb.bulkTransfer(handle, READ_ENDPOINT, buffer, transferred, timeout);
        int transferredBytes = transferred.get(0);

        // libusb may report a timeout after already receiving a partial response.
        // The original ScreenDUO driver processes those bytes, so preserve them.
        if (transferredBytes > 0) {
            byte[] response = new byte[transferredBytes];
            buffer.rewind();
            buffer.get(response);
            return new UsbReadResult(result, response);
        }

        if (result != LibUsb.SUCCESS
                && result != LibUsb.ERROR_TIMEOUT
                && result != LibUsb.ERROR_PIPE) {
            ensureSuccess(result, message);
        }
        return new UsbReadResult(result, new byte[0]);
    }

    private void readStatus() {
        UsbReadResult status = readUsb(
                ScreenDuoProtocol.STATUS_SIZE,
                STATUS_TIMEOUT_MILLIS,
                "Unable to read ScreenDUO block status");

        if (status.status() == LibUsb.ERROR_PIPE && status.data().length == 0) {
            int clearHaltStatus = LibUsb.clearHalt(handle, READ_ENDPOINT);
            ensureSuccess(clearHaltStatus, "Unable to recover ScreenDUO block status endpoint");
            status = readUsb(
                    ScreenDuoProtocol.STATUS_SIZE,
                    STATUS_TIMEOUT_MILLIS,
                    "Unable to read ScreenDUO block status after recovery");
        }

        if (status.data().length == ScreenDuoProtocol.STATUS_SIZE) {
            return;
        }
        ensureSuccess(status.status(), "Unable to read ScreenDUO block status");
        throw new IllegalStateException(
                "Invalid ScreenDUO status length: " + status.data().length);
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("ScreenDUO is already closed");
        }
    }

    private static Optional<OpenedDevice> findAndOpen(Context context) {
        DeviceList devices = new DeviceList();
        int result = LibUsb.getDeviceList(context, devices);
        if (result < LibUsb.SUCCESS) {
            throw new LibUsbException("Unable to enumerate USB devices", result);
        }

        try {
            for (Device device : devices) {
                DeviceDescriptor descriptor = new DeviceDescriptor();
                result = LibUsb.getDeviceDescriptor(device, descriptor);
                ensureSuccess(result, "Unable to read a USB device descriptor");

                if (matches(descriptor.idVendor(), descriptor.idProduct())) {
                    return Optional.of(openDevice(device, descriptor));
                }
            }

            return Optional.empty();
        } finally {
            LibUsb.freeDeviceList(devices, true);
        }
    }

    private static OpenedDevice openDevice(Device device, DeviceDescriptor descriptor) {
        String descriptorReport = UsbDescriptorReport.create(device, descriptor);
        DeviceHandle handle = new DeviceHandle();
        int result = LibUsb.open(device, handle);
        ensureSuccess(result, "ScreenDUO found but could not be opened");

        boolean claimed = false;
        try {
            result = LibUsb.claimInterface(handle, INTERFACE_NUMBER);
            ensureSuccess(result, "ScreenDUO found but interface 0 could not be claimed");
            claimed = true;

            result = LibUsb.setInterfaceAltSetting(
                    handle, INTERFACE_NUMBER, ALTERNATE_SETTING);
            ensureSuccess(result, "Unable to select ScreenDUO interface alternate setting");

            result = LibUsb.clearHalt(handle, WRITE_ENDPOINT);
            ensureSuccess(result, "Unable to clear ScreenDUO write endpoint");

            result = LibUsb.clearHalt(handle, READ_ENDPOINT);
            ensureSuccess(result, "Unable to clear ScreenDUO read endpoint");

            return new OpenedDevice(handle, descriptorReport);
        } catch (RuntimeException exception) {
            if (claimed) {
                LibUsb.releaseInterface(handle, INTERFACE_NUMBER);
            }
            LibUsb.close(handle);
            throw exception;
        }
    }

    private static void ensureSuccess(int result, String message) {
        if (result != LibUsb.SUCCESS) {
            throw new LibUsbException(message, result);
        }
    }

    private record UsbReadResult(int status, byte[] data) {
    }

    private record OpenedDevice(DeviceHandle handle, String descriptorReport) {
    }
}
