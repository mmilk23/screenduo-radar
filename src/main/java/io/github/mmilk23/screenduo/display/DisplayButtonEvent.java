package io.github.mmilk23.screenduo.display;

public record DisplayButtonEvent(DisplayButton button, int sourceCode) {

    public DisplayButtonEvent {
        if (button == null) {
            throw new IllegalArgumentException("Button must not be null");
        }
    }
}
