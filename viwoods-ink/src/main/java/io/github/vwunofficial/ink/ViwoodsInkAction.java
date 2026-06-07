package io.github.vwunofficial.ink;

import android.view.MotionEvent;

public enum ViwoodsInkAction {
    DOWN,
    MOVE,
    UP,
    CANCEL,
    HOVER_ENTER,
    HOVER_MOVE,
    HOVER_EXIT,
    OTHER;

    public static ViwoodsInkAction fromAndroidAction(int action) {
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return DOWN;
            case MotionEvent.ACTION_MOVE:
                return MOVE;
            case MotionEvent.ACTION_UP:
                return UP;
            case MotionEvent.ACTION_CANCEL:
                return CANCEL;
            case MotionEvent.ACTION_HOVER_ENTER:
                return HOVER_ENTER;
            case MotionEvent.ACTION_HOVER_MOVE:
                return HOVER_MOVE;
            case MotionEvent.ACTION_HOVER_EXIT:
                return HOVER_EXIT;
            default:
                return OTHER;
        }
    }
}
