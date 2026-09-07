package io.github.mmilk23.screenduo.device;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.usb4java.LibUsb;

class UsbDescriptorReportTest {

    @Test
    void identifiesEndpointDirection() {
        assertEquals("IN", UsbDescriptorReport.endpointDirection((byte) 0x81));
        assertEquals("OUT", UsbDescriptorReport.endpointDirection((byte) 0x01));
    }

    @Test
    void identifiesTransferType() {
        assertEquals("CONTROL", UsbDescriptorReport.transferType(LibUsb.TRANSFER_TYPE_CONTROL));
        assertEquals("ISOCHRONOUS", UsbDescriptorReport.transferType(LibUsb.TRANSFER_TYPE_ISOCHRONOUS));
        assertEquals("BULK", UsbDescriptorReport.transferType(LibUsb.TRANSFER_TYPE_BULK));
        assertEquals("INTERRUPT", UsbDescriptorReport.transferType(LibUsb.TRANSFER_TYPE_INTERRUPT));
    }

    @Test
    void formatsBinaryCodedUsbVersion() {
        assertEquals("2.00", UsbDescriptorReport.formatBcd((short) 0x0200));
        assertEquals("1.10", UsbDescriptorReport.formatBcd((short) 0x0110));
    }
}
