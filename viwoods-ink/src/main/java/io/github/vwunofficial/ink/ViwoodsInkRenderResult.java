package io.github.vwunofficial.ink;

import android.graphics.Rect;

public final class ViwoodsInkRenderResult {
    public enum Status {
        RENDERED,
        SKIPPED_EMPTY_RECT,
        FAILED
    }

    public final boolean rendered;
    public final Status status;
    public final Rect screenRect;
    public final long elapsedNanos;
    public final String detail;

    private ViwoodsInkRenderResult(boolean rendered, Status status, Rect screenRect,
                                   long elapsedNanos, String detail) {
        this.rendered = rendered;
        this.status = status;
        this.screenRect = screenRect == null ? new Rect() : new Rect(screenRect);
        this.elapsedNanos = elapsedNanos;
        this.detail = detail == null ? "" : detail;
    }

    static ViwoodsInkRenderResult rendered(Rect screenRect, long elapsedNanos) {
        return new ViwoodsInkRenderResult(true, Status.RENDERED, screenRect, elapsedNanos, "");
    }

    static ViwoodsInkRenderResult skippedEmptyRect(Rect screenRect) {
        return new ViwoodsInkRenderResult(false, Status.SKIPPED_EMPTY_RECT, screenRect, -1L,
                "No non-empty screen rect to render");
    }

    static ViwoodsInkRenderResult failed(Rect screenRect, long elapsedNanos, String detail) {
        return new ViwoodsInkRenderResult(false, Status.FAILED, screenRect, elapsedNanos, detail);
    }
}
