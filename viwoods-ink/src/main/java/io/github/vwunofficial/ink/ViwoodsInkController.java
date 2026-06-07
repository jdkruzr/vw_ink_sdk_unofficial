package io.github.vwunofficial.ink;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

public final class ViwoodsInkController {
    private final View view;
    private final ViwoodsBitmapProvider bitmapProvider;
    private final ViwoodsInkRenderer renderer;
    private final ViwoodsInkConfig config;
    private final ViwoodsHiddenEnote enote;
    private final Rect localRenderRect = new Rect();
    private final Rect screenRect = new Rect();
    private final Rect batchRect = new Rect();
    private final int[] screenOffset = new int[2];
    private boolean running;
    private boolean strokeActive;
    private int batchedRects;
    private ViwoodsInkRenderResult lastRenderResult;

    public ViwoodsInkController(View view, ViwoodsBitmapProvider bitmapProvider,
                                ViwoodsInkRenderer renderer) {
        this(view, bitmapProvider, renderer, ViwoodsInkConfig.defaults(), ViwoodsInkLogger.NONE);
    }

    public ViwoodsInkController(View view, ViwoodsBitmapProvider bitmapProvider,
                                ViwoodsInkRenderer renderer, ViwoodsInkConfig config,
                                ViwoodsInkLogger logger) {
        if (view == null) {
            throw new IllegalArgumentException("view == null");
        }
        if (bitmapProvider == null) {
            throw new IllegalArgumentException("bitmapProvider == null");
        }
        if (renderer == null) {
            throw new IllegalArgumentException("renderer == null");
        }
        this.view = view;
        this.bitmapProvider = bitmapProvider;
        this.renderer = renderer;
        this.config = config == null ? ViwoodsInkConfig.defaults() : config;
        this.enote = new ViwoodsHiddenEnote(logger);
    }

    public ViwoodsInkAvailability availability() {
        return enote.availability();
    }

    public boolean isAvailable() {
        return availability().available;
    }

    public boolean start() {
        return startWithResult().started;
    }

    public ViwoodsInkStartResult startDisplayOnlyWithResult() {
        ViwoodsInkStartResult result = configureForStart();
        if (!result.started) {
            return result;
        }
        boolean ok = enote.setWritingEnabled(true);
        if (!ok) {
            enote.release();
            running = false;
            return ViwoodsInkStartResult.failed(
                    ViwoodsInkStartResult.Status.WRITING_ENABLE_FAILED,
                    "Failed to enable Viwoods writing");
        }
        running = true;
        return ViwoodsInkStartResult.started("Viwoods display-only ink started");
    }

    public ViwoodsInkStartResult startWithResult() {
        ViwoodsInkStartResult result = configureForStart();
        if (!result.started) {
            return result;
        }
        boolean ok = enote.setInputSink(new ViwoodsHiddenEnote.NativeInputSink() {
            @Override
            public void onNativeInput(final int x, final int y, final int pressureValue,
                                      final float tilt, final int toolType, final int action,
                                      final int actionButton, final int buttonState,
                                      final long callbackNanos) {
                view.post(new Runnable() {
                    @Override
                    public void run() {
                        handleNativeInput(x, y, pressureValue, tilt, toolType, action,
                                actionButton, buttonState, callbackNanos);
                    }
                });
            }
        });
        if (ok) {
            enote.setWritingEnabled(true);
            running = true;
            return ViwoodsInkStartResult.started("Viwoods ink started");
        }
        enote.release();
        return ViwoodsInkStartResult.failed(
                ViwoodsInkStartResult.Status.LISTENER_REGISTRATION_FAILED,
                "Failed to register Viwoods native input listener");
    }

    private ViwoodsInkStartResult configureForStart() {
        if (running) {
            return ViwoodsInkStartResult.alreadyRunning();
        }
        ViwoodsInkAvailability availability = availability();
        if (!availability.available) {
            return ViwoodsInkStartResult.failed(ViwoodsInkStartResult.Status.UNAVAILABLE,
                    availability.detail);
        }
        if (view.getWidth() <= 0 || view.getHeight() <= 0) {
            return ViwoodsInkStartResult.failed(ViwoodsInkStartResult.Status.VIEW_NOT_READY,
                    "View has no measured size");
        }
        updateScreenOffset();
        Bitmap bitmap = bitmapProvider.getInkBitmap();
        if (!isUsableBitmap(bitmap)) {
            return ViwoodsInkStartResult.failed(ViwoodsInkStartResult.Status.BITMAP_UNAVAILABLE,
                    "Bitmap provider returned null or recycled bitmap");
        }
        boolean configured = enote.configureBitmap(bitmap, orientation(), screenOffset[0], screenOffset[1],
                config.jumpPointCount, config.renderDelayCount);
        if (!configured) {
            return ViwoodsInkStartResult.failed(
                    ViwoodsInkStartResult.Status.BITMAP_CONFIGURATION_FAILED,
                    "Failed to configure Viwoods Java bitmap");
        }
        return ViwoodsInkStartResult.started("Viwoods ink configured");
    }

    public boolean isRunning() {
        return running;
    }

    public ViwoodsInkState state() {
        return running ? ViwoodsInkState.RUNNING : ViwoodsInkState.STOPPED;
    }

    public ViwoodsInkRenderResult lastRenderResult() {
        return lastRenderResult;
    }

    public ViwoodsInkRenderResult renderNow(Rect localDirty) {
        if (!running) {
            return ViwoodsInkRenderResult.failed(localDirty, -1L, "Controller is not running");
        }
        if (localDirty == null || localDirty.isEmpty()) {
            lastRenderResult = ViwoodsInkRenderResult.skippedEmptyRect(localDirty);
            notifyRenderResult(lastRenderResult);
            return lastRenderResult;
        }
        batchRect.setEmpty();
        batchedRects = 0;
        if (!prepareScreenRect(localDirty)) {
            lastRenderResult = ViwoodsInkRenderResult.skippedEmptyRect(localRenderRect);
            notifyRenderResult(lastRenderResult);
            return lastRenderResult;
        }
        batchRect.set(screenRect);
        renderBatch();
        batchRect.setEmpty();
        return lastRenderResult;
    }

    public boolean beginStroke() {
        if (!running) {
            return false;
        }
        strokeActive = true;
        batchedRects = 0;
        batchRect.setEmpty();
        boolean ok = enote.onWritingStart();
        Bitmap bitmap = bitmapProvider.getInkBitmap();
        if (isUsableBitmap(bitmap)) {
            updateScreenOffset();
            ok &= enote.configureWritingBitmap(bitmap, orientation(), screenOffset[0], screenOffset[1]);
        }
        return ok;
    }

    public boolean endStroke() {
        if (!running) {
            return false;
        }
        flushDirty();
        strokeActive = false;
        return enote.onWritingEnd();
    }

    public boolean setDisplayMode(ViwoodsEinkMode mode) {
        if (mode == null) {
            return false;
        }
        return enote.setPictureMode(mode.value);
    }

    public boolean refreshBitmap() {
        if (!running) {
            return false;
        }
        updateScreenOffset();
        Bitmap bitmap = bitmapProvider.getInkBitmap();
        if (!isUsableBitmap(bitmap)) {
            return false;
        }
        return enote.configureBitmap(bitmap, orientation(), screenOffset[0], screenOffset[1],
                config.jumpPointCount, config.renderDelayCount);
    }

    public boolean refreshWritingBitmap() {
        if (!running) {
            return false;
        }
        updateScreenOffset();
        Bitmap bitmap = bitmapProvider.getInkBitmap();
        if (!isUsableBitmap(bitmap)) {
            return false;
        }
        return enote.configureWritingBitmap(bitmap, orientation(), screenOffset[0], screenOffset[1]);
    }

    public boolean refreshBackgroundBitmap() {
        if (!running) {
            return false;
        }
        updateScreenOffset();
        Bitmap bitmap = bitmapProvider.getInkBitmap();
        if (!isUsableBitmap(bitmap)) {
            return false;
        }
        return enote.configureBackgroundBitmap(bitmap, orientation(), screenOffset[0], screenOffset[1]);
    }

    public void stop() {
        running = false;
        strokeActive = false;
        batchedRects = 0;
        batchRect.setEmpty();
        enote.release();
    }

    public void detach() {
        stop();
    }

    private void handleNativeInput(int rawX, int rawY, int pressureValue, float tilt, int toolType,
                                   int rawAction, int actionButton, int buttonState,
                                   long callbackNanos) {
        if (!running) {
            return;
        }
        int action = normalizeAction(rawAction, pressureValue);
        if (action == rawAction && (action == MotionEvent.ACTION_HOVER_ENTER
                || action == MotionEvent.ACTION_HOVER_MOVE
                || action == MotionEvent.ACTION_HOVER_EXIT)) {
            return;
        }

        updateScreenOffset();
        ViwoodsInkEvent event = new ViwoodsInkEvent(
                rawX - screenOffset[0],
                rawY - screenOffset[1],
                ViwoodsInkAction.fromAndroidAction(action),
                action,
                rawAction,
                pressureValue,
                Math.max(0.1f, pressureValue / 1024f),
                tilt,
                toolType,
                actionButton,
                buttonState,
                callbackNanos);

        if (event.isDown()) {
            strokeActive = true;
            batchedRects = 0;
            batchRect.setEmpty();
            enote.onWritingStart();
            Bitmap bitmap = bitmapProvider.getInkBitmap();
            if (isUsableBitmap(bitmap)) {
                enote.configureWritingBitmap(bitmap, orientation(), screenOffset[0], screenOffset[1]);
            }
            notifyStrokeStart(event);
        }

        Rect dirty = renderer.onInkEvent(event);
        if (dirty != null && !dirty.isEmpty()) {
            Rect renderedLocalDirty = renderDirty(dirty, event.isUpOrCancel());
            if (config.invalidateView) {
                view.invalidate(renderedLocalDirty);
            }
        }

        if (event.isUpOrCancel()) {
            flushDirty();
            enote.onWritingEnd();
            strokeActive = false;
            notifyStrokeEnd(event);
        }
    }

    private int normalizeAction(int rawAction, int pressureValue) {
        if (rawAction == MotionEvent.ACTION_HOVER_MOVE && (strokeActive || pressureValue > 0)) {
            return strokeActive ? MotionEvent.ACTION_MOVE : MotionEvent.ACTION_DOWN;
        }
        return rawAction;
    }

    private Rect renderDirty(Rect localDirty, boolean force) {
        if (!prepareScreenRect(localDirty)) {
            return localRenderRect;
        }
        if (batchRect.isEmpty()) {
            batchRect.set(screenRect);
        } else {
            batchRect.union(screenRect);
        }
        batchedRects++;
        if (force || batchedRects >= config.renderBatchSize) {
            renderBatch();
            batchRect.setEmpty();
            batchedRects = 0;
        }
        return localRenderRect;
    }

    private boolean prepareScreenRect(Rect localDirty) {
        localRenderRect.set(localDirty);
        if (config.dirtyRectPaddingPx > 0) {
            localRenderRect.inset(-config.dirtyRectPaddingPx, -config.dirtyRectPaddingPx);
        }
        if (config.clipDirtyRectsToView && !localRenderRect.intersect(0, 0, view.getWidth(), view.getHeight())) {
            return false;
        }
        screenRect.set(localRenderRect);
        screenRect.offset(screenOffset[0], screenOffset[1]);
        return !screenRect.isEmpty();
    }

    private void flushDirty() {
        if (!batchRect.isEmpty()) {
            renderBatch();
            batchRect.setEmpty();
            batchedRects = 0;
        }
    }

    private void renderBatch() {
        lastRenderResult = enote.render(batchRect);
        if (lastRenderResult != null) {
            notifyRenderResult(lastRenderResult);
            if (lastRenderResult.status == ViwoodsInkRenderResult.Status.FAILED) {
                notifyRenderFailure(lastRenderResult);
            }
        }
    }

    private void notifyStrokeStart(ViwoodsInkEvent event) {
        try {
            config.listener.onStrokeStart(event);
        } catch (Throwable ignored) {
        }
    }

    private void notifyStrokeEnd(ViwoodsInkEvent event) {
        try {
            config.listener.onStrokeEnd(event);
        } catch (Throwable ignored) {
        }
    }

    private void notifyRenderResult(ViwoodsInkRenderResult result) {
        try {
            config.listener.onRenderResult(result);
        } catch (Throwable ignored) {
        }
    }

    private void notifyRenderFailure(ViwoodsInkRenderResult result) {
        try {
            config.listener.onRenderFailure(result);
        } catch (Throwable ignored) {
        }
    }

    private void updateScreenOffset() {
        view.getLocationOnScreen(screenOffset);
    }

    private int orientation() {
        return view.getResources().getConfiguration().orientation;
    }

    private static boolean isUsableBitmap(Bitmap bitmap) {
        return bitmap != null && !bitmap.isRecycled();
    }
}
