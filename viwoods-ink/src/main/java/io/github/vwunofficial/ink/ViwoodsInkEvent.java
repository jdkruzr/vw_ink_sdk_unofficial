package io.github.vwunofficial.ink;

public final class ViwoodsInkEvent {
    /** Absolute coordinates received directly from ENoteWriting. Useful for diagnostics. */
    public final int rawX;
    public final int rawY;
    /** View origin used to translate this callback into local {@link #x}/{@link #y}. */
    public final int screenOffsetX;
    public final int screenOffsetY;
    public final float x;
    public final float y;
    public final ViwoodsInkAction actionType;
    public final int action;
    public final int rawAction;
    public final int pressureValue;
    public final float pressure;
    public final float tilt;
    public final int toolType;
    public final int actionButton;
    public final int buttonState;
    public final long eventNanos;

    ViwoodsInkEvent(int rawX, int rawY, int screenOffsetX, int screenOffsetY,
                    float x, float y, ViwoodsInkAction actionType, int action, int rawAction,
                    int pressureValue, float pressure, float tilt, int toolType,
                    int actionButton, int buttonState, long eventNanos) {
        this.rawX = rawX;
        this.rawY = rawY;
        this.screenOffsetX = screenOffsetX;
        this.screenOffsetY = screenOffsetY;
        this.x = x;
        this.y = y;
        this.actionType = actionType;
        this.action = action;
        this.rawAction = rawAction;
        this.pressureValue = pressureValue;
        this.pressure = pressure;
        this.tilt = tilt;
        this.toolType = toolType;
        this.actionButton = actionButton;
        this.buttonState = buttonState;
        this.eventNanos = eventNanos;
    }

    public boolean isDown() {
        return actionType == ViwoodsInkAction.DOWN;
    }

    public boolean isMove() {
        return actionType == ViwoodsInkAction.MOVE;
    }

    public boolean isUpOrCancel() {
        return actionType == ViwoodsInkAction.UP || actionType == ViwoodsInkAction.CANCEL;
    }
}
