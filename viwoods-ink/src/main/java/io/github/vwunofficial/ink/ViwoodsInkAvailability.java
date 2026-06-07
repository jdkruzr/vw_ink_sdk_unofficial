package io.github.vwunofficial.ink;

public final class ViwoodsInkAvailability {
    public enum Status {
        AVAILABLE,
        ENOTE_SETTING_NOT_FOUND
    }

    public final boolean available;
    public final Status status;
    public final String detail;

    ViwoodsInkAvailability(boolean available, Status status, String detail) {
        this.available = available;
        this.status = status;
        this.detail = detail;
    }
}
