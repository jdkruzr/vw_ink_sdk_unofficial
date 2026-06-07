package io.github.vwunofficial.ink;

import android.graphics.Rect;

@FunctionalInterface
public interface ViwoodsInkRenderer {
    /**
     * Draws the event into the app-owned bitmap and returns the local dirty rect.
     * Return null or an empty rect when no pixels changed.
     */
    Rect onInkEvent(ViwoodsInkEvent event);
}
