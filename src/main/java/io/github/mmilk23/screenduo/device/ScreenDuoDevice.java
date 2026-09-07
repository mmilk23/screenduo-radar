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
    public void close() {
        if (closed) {
            return;
        }

        LibUsb.close(handle);
        LibUsb.exit(context);
        closed = true;
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
                    String descriptorReport = UsbDescriptorReport.create(device, descriptor);
                    DeviceHandle handle = new DeviceHandle();
                    result = LibUsb.open(device, handle);
                    ensureSuccess(result, "ScreenDUO found but could not be opened");
                    return Optional.of(new OpenedDevice(handle, descriptorReport));
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

    private record OpenedDevice(DeviceHandle handle, String descriptorReport) {
    }
}
