package io.github.mmilk23.screenduo.device;

import org.usb4java.ConfigDescriptor;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.EndpointDescriptor;
import org.usb4java.Interface;
import org.usb4java.InterfaceDescriptor;
import org.usb4java.LibUsb;
import org.usb4java.LibUsbException;

final class UsbDescriptorReport {

    private UsbDescriptorReport() {
    }

    static String create(Device device, DeviceDescriptor deviceDescriptor) {
        StringBuilder report = new StringBuilder();
        report.append(System.lineSeparator())
                .append("USB descriptor report (read-only)")
                .append(System.lineSeparator())
                .append(String.format(
                        "Device %04x:%04x | USB %s | class 0x%02x | subclass 0x%02x | protocol 0x%02x | configurations %d%n",
                        Short.toUnsignedInt(deviceDescriptor.idVendor()),
                        Short.toUnsignedInt(deviceDescriptor.idProduct()),
                        formatBcd(deviceDescriptor.bcdUSB()),
                        Byte.toUnsignedInt(deviceDescriptor.bDeviceClass()),
                        Byte.toUnsignedInt(deviceDescriptor.bDeviceSubClass()),
                        Byte.toUnsignedInt(deviceDescriptor.bDeviceProtocol()),
                        Byte.toUnsignedInt(deviceDescriptor.bNumConfigurations())));

        int configurationCount = Byte.toUnsignedInt(deviceDescriptor.bNumConfigurations());
        for (int configurationIndex = 0; configurationIndex < configurationCount; configurationIndex++) {
            appendConfiguration(report, device, configurationIndex);
        }

        return report.toString();
    }

    private static void appendConfiguration(StringBuilder report, Device device, int configurationIndex) {
        ConfigDescriptor configuration = new ConfigDescriptor();
        int result = LibUsb.getConfigDescriptor(device, (byte) configurationIndex, configuration);
        if (result != LibUsb.SUCCESS) {
            throw new LibUsbException(
                    "Unable to read ScreenDUO USB configuration " + configurationIndex, result);
        }

        try {
            report.append(String.format(
                    "Configuration index %d | value %d | interfaces %d | attributes 0x%02x | maxPowerRaw %d%n",
                    configurationIndex,
                    Byte.toUnsignedInt(configuration.bConfigurationValue()),
                    Byte.toUnsignedInt(configuration.bNumInterfaces()),
                    Byte.toUnsignedInt(configuration.bmAttributes()),
                    Byte.toUnsignedInt(configuration.bMaxPower())));

            for (Interface usbInterface : configuration.iface()) {
                for (InterfaceDescriptor interfaceDescriptor : usbInterface.altsetting()) {
                    appendInterface(report, interfaceDescriptor);
                }
            }
        } finally {
            LibUsb.freeConfigDescriptor(configuration);
        }
    }

    private static void appendInterface(StringBuilder report, InterfaceDescriptor descriptor) {
        report.append(String.format(
                "  Interface %d | alternate %d | class 0x%02x | subclass 0x%02x | protocol 0x%02x | endpoints %d%n",
                Byte.toUnsignedInt(descriptor.bInterfaceNumber()),
                Byte.toUnsignedInt(descriptor.bAlternateSetting()),
                Byte.toUnsignedInt(descriptor.bInterfaceClass()),
                Byte.toUnsignedInt(descriptor.bInterfaceSubClass()),
                Byte.toUnsignedInt(descriptor.bInterfaceProtocol()),
                Byte.toUnsignedInt(descriptor.bNumEndpoints())));

        for (EndpointDescriptor endpoint : descriptor.endpoint()) {
            report.append(String.format(
                    "    Endpoint 0x%02x | %s | %s | maxPacketSize %d | interval %d%n",
                    Byte.toUnsignedInt(endpoint.bEndpointAddress()),
                    endpointDirection(endpoint.bEndpointAddress()),
                    transferType(endpoint.bmAttributes()),
                    Short.toUnsignedInt(endpoint.wMaxPacketSize()),
                    Byte.toUnsignedInt(endpoint.bInterval())));
        }
    }

    static String endpointDirection(byte endpointAddress) {
        return (endpointAddress & LibUsb.ENDPOINT_DIR_MASK) == LibUsb.ENDPOINT_IN ? "IN" : "OUT";
    }

    static String transferType(byte attributes) {
        return switch (attributes & LibUsb.TRANSFER_TYPE_MASK) {
            case LibUsb.TRANSFER_TYPE_CONTROL -> "CONTROL";
            case LibUsb.TRANSFER_TYPE_ISOCHRONOUS -> "ISOCHRONOUS";
            case LibUsb.TRANSFER_TYPE_BULK -> "BULK";
            case LibUsb.TRANSFER_TYPE_INTERRUPT -> "INTERRUPT";
            default -> "UNKNOWN";
        };
    }

    static String formatBcd(short value) {
        int unsignedValue = Short.toUnsignedInt(value);
        return String.format("%x.%02x", unsignedValue >> 8, unsignedValue & 0xff);
    }
}
