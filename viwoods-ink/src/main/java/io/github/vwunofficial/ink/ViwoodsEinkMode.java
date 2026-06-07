package io.github.vwunofficial.ink;

public enum ViwoodsEinkMode {
    GL16(3),
    FAST(4),
    GC(17);

    public final int value;

    ViwoodsEinkMode(int value) {
        this.value = value;
    }
}
