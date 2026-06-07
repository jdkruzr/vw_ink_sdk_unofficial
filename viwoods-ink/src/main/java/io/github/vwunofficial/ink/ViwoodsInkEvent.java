package io.github.vwunofficial.ink;

public final class ViwoodsInkEvent {
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

    ViwoodsInkEvent(float x, float y, ViwoodsInkAction actionType, int action, int rawAction,
                    int pressureValue, float pressure, float tilt, int toolType,
                    int actionButton, int buttonState, long eventNanos) {
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
