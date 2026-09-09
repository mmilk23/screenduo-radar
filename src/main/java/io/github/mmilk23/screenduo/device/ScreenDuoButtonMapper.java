package io.github.mmilk23.screenduo.device;

import io.github.mmilk23.screenduo.display.DisplayButton;
import io.github.mmilk23.screenduo.display.DisplayButtonEvent;

final class ScreenDuoButtonMapper {

    private ScreenDuoButtonMapper() {
    }

    static DisplayButtonEvent map(int code) {
        DisplayButton button = switch (code) {
            case 0 -> DisplayButton.CONFIRM;
            case 1 -> DisplayButton.LEFT;
            case 2 -> DisplayButton.RIGHT;
            case 3 -> DisplayButton.UP;
            case 4 -> DisplayButton.DOWN;
            case 6 -> DisplayButton.BACK;
            case 12 -> DisplayButton.ACTION_1;
            case 13 -> DisplayButton.ACTION_2;
            default -> DisplayButton.UNKNOWN;
        };
        return new DisplayButtonEvent(button, code);
    }
}
