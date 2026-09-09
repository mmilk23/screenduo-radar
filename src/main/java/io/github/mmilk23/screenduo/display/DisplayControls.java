package io.github.mmilk23.screenduo.display;

import java.util.Optional;

public interface DisplayControls {

    Optional<DisplayButtonEvent> pollButton();
}
