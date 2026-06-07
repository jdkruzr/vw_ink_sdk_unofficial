package io.github.vwunofficial.ink;

import android.view.MotionEvent;

public final class ViwoodsInkEvent {
    public final float x;
    public final float y;
    public final int action;
    public final int rawAction;
    public final int pressureValue;
    public final float pressure;
    public final float tilt;
    public final int toolType;
    public final int actionButton;
    public final int buttonState;
    public final long eventNanos;

    ViwoodsInkEvent(float x, float y, int action, int rawAction, int pressureValue,
                    float pressure, float tilt, int toolType, int actionButton,
                    int buttonState, long eventNanos) {
        this.x = x;
        this.y = y;
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
        return action == MotionEvent.ACTION_DOWN;
    }

    public boolean isMove() {
        return action == MotionEvent.ACTION_MOVE;
    }

    public boolean isUpOrCancel() {
        return action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL;
    }
}
