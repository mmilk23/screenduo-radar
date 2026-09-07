package io.github.mmilk23.screenduo.device;

import java.util.Optional;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

public final class ScreenDuoDevice implements AutoCloseable {

    public static final short VENDOR_ID = (short) 0x1043;
    public static final short PRODUCT_ID = (short) 0x3100;

    private final Context context;
    private final DeviceHandle handle;
    private boolean closed;

    private ScreenDuoDevice(Context context, DeviceHandle handle) {
        this.context = context;
        this.handle = handle;
    }

    public static Optional<ScreenDuoDevice> open() {
        Context context = new Context();
        int result = LibUsb.init(context);
        ensureSuccess(result, "Unable to initialize libusb");

        try {
            Optional<DeviceHandle> detectedHandle = findAndOpen(context);
            if (detectedHandle.isPresent()) {
                return Optional.of(new ScreenDuoDevice(context, detectedHandle.orElseThrow()));
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

    @Override
    public void close() {
        if (closed) {
            return;
        }

        LibUsb.close(handle);
        LibUsb.exit(context);
        closed = true;
    }

    private static Optional<DeviceHandle> findAndOpen(Context context) {
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
                    DeviceHandle handle = new DeviceHandle();
                    result = LibUsb.open(device, handle);
                    ensureSuccess(result, "ScreenDUO found but could not be opened");
                    return Optional.of(handle);
                }
            }

            return Optional.empty();
        } finally {
            LibUsb.freeDeviceList(devices, true);
        }
    }

    private static void ensureSuccess(int result, String message) {
        if (result != LibUsb.SUCCESS) {
            throw new LibUsbException(message, result);
        }
    }
}
