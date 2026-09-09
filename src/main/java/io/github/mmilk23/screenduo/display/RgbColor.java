package io.github.mmilk23.screenduo.display;

public record RgbColor(int red, int green, int blue) {

    public RgbColor {
        requireChannel(red);
        requireChannel(green);
        requireChannel(blue);
    }

    private static void requireChannel(int value) {
        if (value < 0 || value > 255) {
            throw new IllegalArgumentException("RGB channels must be between 0 and 255.");
        }
    }
}
