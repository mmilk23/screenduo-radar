package io.github.mmilk23.screenduo.data;

import java.io.IOException;
import java.nio.file.Path;

@FunctionalInterface
public interface TextDataSource {

    Path get() throws IOException, InterruptedException;
}
