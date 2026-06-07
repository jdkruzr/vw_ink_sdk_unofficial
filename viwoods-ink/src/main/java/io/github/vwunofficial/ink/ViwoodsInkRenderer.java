package io.github.vwunofficial.ink;

import android.graphics.Rect;

@FunctionalInterface
public interface ViwoodsInkRenderer {
    /**
     * Draws the event into the app-owned bitmap and returns the local dirty rect.
     * Return null or an empty rect when no pixels changed.
     *
     * <p>Callbacks are currently delivered on the associated view's UI thread.
     * The returned rect must be in local view coordinates; the controller converts
     * it to screen coordinates before calling the Viwoods render API.</p>
     */
    Rect onInkEvent(ViwoodsInkEvent event);
}
