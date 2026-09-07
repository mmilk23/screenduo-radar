package io.github.mmilk23.screenduo;

import io.github.mmilk23.screenduo.device.ScreenDuoDevice;
import io.github.mmilk23.screenduo.display.ClassicTvTestPattern;
import io.github.mmilk23.screenduo.display.Display;
import io.github.mmilk23.screenduo.display.DisplayButtonEvent;
import io.github.mmilk23.screenduo.display.DisplayControls;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import org.usb4java.LibUsbException;

public final class ScreenDuoApplication {

    private static final String TEST_PATTERN_ARGUMENT = "--test-pattern";
    private static final String BUTTON_TEST_ARGUMENT = "--button-test";
    private static final Duration BUTTON_TEST_DURATION = Duration.ofSeconds(30);

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

                boolean testPatternRequested = hasArgument(args, TEST_PATTERN_ARGUMENT);
                boolean buttonTestRequested = hasArgument(args, BUTTON_TEST_ARGUMENT);

                if (testPatternRequested || buttonTestRequested) {
                    showTestPattern(device);
                }
                if (buttonTestRequested) {
                    testButtons(device);
                }
                if (!testPatternRequested && !buttonTestRequested) {
                    System.out.println(
                            "Diagnostic mode only. Use --test-pattern or --button-test.");
                }
            }
        } catch (LibUsbException | IllegalStateException exception) {
            System.err.println("Unable to access the ScreenDUO: " + exception.getMessage());
            System.exit(1);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            System.err.println("Button test interrupted.");
        }
    }

    private static boolean hasArgument(String[] args, String expected) {
        return Arrays.asList(args).contains(expected);
    }

    private static void showTestPattern(Display display) {
        display.show(ClassicTvTestPattern.render(display.geometry()));
        System.out.println("Classic TV test pattern sent successfully.");
    }

    private static void testButtons(DisplayControls controls) throws InterruptedException {
        Instant deadline = Instant.now().plus(BUTTON_TEST_DURATION);
        System.out.println(
                "Button test active for 30 seconds. Press the ScreenDUO buttons (Ctrl+C to stop).");

        while (Instant.now().isBefore(deadline)) {
            Optional<DisplayButtonEvent> event = controls.pollButton();
            event.ifPresent(button -> System.out.printf(
                    "Button: %-8s | ScreenDUO code: %d%n",
                    button.button(),
                    button.sourceCode()));
            Thread.sleep(100);
        }

        System.out.println("Button test finished.");
    }
}
