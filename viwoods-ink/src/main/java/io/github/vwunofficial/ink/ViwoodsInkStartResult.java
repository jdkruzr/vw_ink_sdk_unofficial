package io.github.vwunofficial.ink;

public final class ViwoodsInkStartResult {
    public enum Status {
        STARTED,
        ALREADY_RUNNING,
        UNAVAILABLE,
        VIEW_NOT_READY,
        BITMAP_UNAVAILABLE,
        BITMAP_CONFIGURATION_FAILED,
        WRITING_ENABLE_FAILED,
        LISTENER_REGISTRATION_FAILED
    }

    public final boolean started;
    public final Status status;
    public final String detail;

    ViwoodsInkStartResult(boolean started, Status status, String detail) {
        this.started = started;
        this.status = status;
        this.detail = detail;
    }

    static ViwoodsInkStartResult started(String detail) {
        return new ViwoodsInkStartResult(true, Status.STARTED, detail);
    }

    static ViwoodsInkStartResult alreadyRunning() {
        return new ViwoodsInkStartResult(true, Status.ALREADY_RUNNING, "Controller is already running");
    }

    static ViwoodsInkStartResult failed(Status status, String detail) {
        return new ViwoodsInkStartResult(false, status, detail);
    }
}
