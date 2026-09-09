package io.github.mmilk23.screenduo.display;

public record DisplayGeometry(int width, int height) {

    public DisplayGeometry {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Display dimensions must be positive");
        }
    }

    public int pixelCount() {
        return Math.multiplyExact(width, height);
    }
}
