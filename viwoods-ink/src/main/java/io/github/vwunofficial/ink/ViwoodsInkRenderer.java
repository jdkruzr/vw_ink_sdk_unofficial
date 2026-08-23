package io.github.vwunofficial.ink;

import android.graphics.Rect;

@FunctionalInterface
public interface ViwoodsInkRenderer {
    /**
     * Draws the event into the app-owned bitmap and returns the local dirty rect.
     * Return null or an empty rect when no pixels changed.
     *
     * <p>Callbacks are delivered on the associated view's UI thread by default. When
     * {@code ViwoodsInkConfig.directInputCallbacks} is enabled they run on Viwoods' ENote worker
     * thread instead; in that mode implementations must be thread-safe and must not mutate Views.
     * The returned rect must be in local view coordinates. The controller applies
     * configured dirty-rect padding/clipping, then converts the rect to screen
     * coordinates before calling the Viwoods render API.</p>
     */
    Rect onInkEvent(ViwoodsInkEvent event);
}
