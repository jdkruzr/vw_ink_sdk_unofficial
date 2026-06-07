package io.github.vwunofficial.ink;

public interface ViwoodsInkListener {
    ViwoodsInkListener NONE = new ViwoodsInkListener() {
    };

    default void onStrokeStart(ViwoodsInkEvent event) {
    }

    default void onStrokeEnd(ViwoodsInkEvent event) {
    }

    default void onRenderFailure(ViwoodsInkRenderResult result) {
    }
}
