package io.github.mmilk23.screenduo.display;

public interface Display extends AutoCloseable {

    DisplayGeometry geometry();

    void show(RgbFrame frame);

    @Override
    void close();
}
