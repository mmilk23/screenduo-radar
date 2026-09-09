package io.github.mmilk23.screenduo.device;

import io.github.mmilk23.screenduo.display.DisplayButtonEvent;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;

public record ScreenDuoButtonProbe(
        int responseStatus,
        byte[] response,
        int clearHaltStatus,
        int drainStatus,
        byte[] drain,
        Optional<DisplayButtonEvent> event) {

    public ScreenDuoButtonProbe {
        response = Arrays.copyOf(response, response.length);
        drain = Arrays.copyOf(drain, drain.length);
        event = event == null ? Optional.empty() : event;
    }

    @Override
    public byte[] response() {
        return Arrays.copyOf(response, response.length);
    }

    @Override
    public byte[] drain() {
        return Arrays.copyOf(drain, drain.length);
    }

    public String fingerprint() {
        return responseStatus + ":" + HexFormat.of().formatHex(response)
                + ":" + clearHaltStatus
                + ":" + drainStatus + ":" + HexFormat.of().formatHex(drain);
    }

    public String describe() {
        return "response(status=" + responseStatus
                + ", bytes=" + response.length
                + ", hex=" + hexadecimal(response)
                + "), clearHalt=" + clearHaltStatus
                + ", drain(status=" + drainStatus
                + ", bytes=" + drain.length
                + ", hex=" + hexadecimal(drain) + ")";
    }

    private static String hexadecimal(byte[] data) {
        return data.length == 0 ? "<empty>" : HexFormat.ofDelimiter(" ").formatHex(data);
    }
}
