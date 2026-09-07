package io.github.mmilk23.screenduo;

import io.github.mmilk23.screenduo.device.ScreenDuoDevice;
import io.github.mmilk23.screenduo.display.ClassicTvTestPattern;
import io.github.mmilk23.screenduo.display.Display;
import java.util.Arrays;
import java.util.Optional;
import org.usb4java.LibUsbException;

public final class ScreenDuoApplication {

    private static final String TEST_PATTERN_ARGUMENT = "--test-pattern";

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

                if (Arrays.asList(args).contains(TEST_PATTERN_ARGUMENT)) {
                    showTestPattern(device);
                } else {
                    System.out.println(
                            "Diagnostic mode only. Use --test-pattern to send the classic TV test image.");
                }
            }
        } catch (LibUsbException | IllegalStateException exception) {
            System.err.println("Unable to access the ScreenDUO: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static void showTestPattern(Display display) {
        display.show(ClassicTvTestPattern.render(display.geometry()));
        System.out.println("Classic TV test pattern sent successfully.");
    }
}
