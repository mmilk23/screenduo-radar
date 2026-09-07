package io.github.mmilk23.screenduo.display;

@FunctionalInterface
public interface ScreenRenderer<T> {

    RgbFrame render(DisplayGeometry geometry, T model);
}
