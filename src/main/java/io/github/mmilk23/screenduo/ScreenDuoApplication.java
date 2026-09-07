package io.github.mmilk23.screenduo;

import io.github.mmilk23.screenduo.device.ScreenDuoDevice;
import java.util.Optional;
import org.usb4java.LibUsbException;

public final class ScreenDuoApplication {

    private ScreenDuoApplication() {
    }

    public static void main(String[] args) {
        try {
            Optional<ScreenDuoDevice> detectedDevice = ScreenDuoDevice.open();

            if (detectedDevice.isEmpty()) {
                System.out.println("ScreenDUO not found (expected USB 1043:3100).");
                return;
            }

            try (ScreenDuoDevice device = detectedDevice.orElseThrow()) {
                System.out.printf(
                        "ScreenDUO detected and opened successfully (%04x:%04x).%n",
                        Short.toUnsignedInt(ScreenDuoDevice.VENDOR_ID),
                        Short.toUnsignedInt(ScreenDuoDevice.PRODUCT_ID));
                System.out.print(device.descriptorReport());
            }
        } catch (LibUsbException exception) {
            System.err.println("Unable to access the ScreenDUO: " + exception.getMessage());
            System.exit(1);
        }
    }
}
