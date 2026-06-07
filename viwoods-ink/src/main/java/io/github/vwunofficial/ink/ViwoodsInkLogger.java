package io.github.vwunofficial.ink;

@FunctionalInterface
public interface ViwoodsInkLogger {
    void log(String message);

    ViwoodsInkLogger NONE = new ViwoodsInkLogger() {
        @Override
        public void log(String message) {
        }
    };
}
