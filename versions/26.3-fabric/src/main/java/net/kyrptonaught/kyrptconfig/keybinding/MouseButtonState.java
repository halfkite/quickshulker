package net.kyrptonaught.kyrptconfig.keybinding;

/** SDL uses one-based button IDs and a bit mask that can contain multiple buttons. */
public final class MouseButtonState {
    private MouseButtonState() {}

    public static boolean isPressed(int state, int button) {
        return button >= 1 && button <= Integer.SIZE && (state & (1 << (button - 1))) != 0;
    }
}
