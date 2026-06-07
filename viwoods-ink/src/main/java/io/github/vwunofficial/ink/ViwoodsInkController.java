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
    private final Rect screenRect = new Rect();
    private final Rect batchRect = new Rect();
    private final int[] screenOffset = new int[2];
    private boolean running;
    private boolean strokeActive;
    private int batchedRects;

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
        if (running) {
            return true;
        }
        ViwoodsInkAvailability availability = availability();
        if (!availability.available) {
            return false;
        }
        updateScreenOffset();
        Bitmap bitmap = bitmapProvider.getInkBitmap();
        enote.configureBitmap(bitmap, orientation(), screenOffset[0], screenOffset[1],
                config.jumpPointCount, config.renderDelayCount);
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
        }
        return ok;
    }

    public boolean isRunning() {
        return running;
    }

    public void refreshBitmap() {
        if (!running) {
            return;
        }
        updateScreenOffset();
        Bitmap bitmap = bitmapProvider.getInkBitmap();
        enote.configureBitmap(bitmap, orientation(), screenOffset[0], screenOffset[1],
                config.jumpPointCount, config.renderDelayCount);
    }

    public void stop() {
        running = false;
        strokeActive = false;
        batchedRects = 0;
        batchRect.setEmpty();
        enote.release();
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
            enote.configureWritingBitmap(bitmapProvider.getInkBitmap(), orientation(),
                    screenOffset[0], screenOffset[1]);
        }

        Rect dirty = renderer.onInkEvent(event);
        if (dirty != null && !dirty.isEmpty()) {
            renderDirty(dirty, event.isUpOrCancel());
            if (config.invalidateView) {
                view.invalidate(dirty);
            }
        }

        if (event.isUpOrCancel()) {
            flushDirty();
            enote.onWritingEnd();
            strokeActive = false;
        }
    }

    private int normalizeAction(int rawAction, int pressureValue) {
        if (rawAction == MotionEvent.ACTION_HOVER_MOVE && (strokeActive || pressureValue > 0)) {
            return strokeActive ? MotionEvent.ACTION_MOVE : MotionEvent.ACTION_DOWN;
        }
        return rawAction;
    }

    private void renderDirty(Rect localDirty, boolean force) {
        screenRect.set(localDirty);
        screenRect.offset(screenOffset[0], screenOffset[1]);
        if (screenRect.isEmpty()) {
            return;
        }
        if (batchRect.isEmpty()) {
            batchRect.set(screenRect);
        } else {
            batchRect.union(screenRect);
        }
        batchedRects++;
        if (force || batchedRects >= config.renderBatchSize) {
            enote.render(batchRect);
            batchRect.setEmpty();
            batchedRects = 0;
        }
    }

    private void flushDirty() {
        if (!batchRect.isEmpty()) {
            enote.render(batchRect);
            batchRect.setEmpty();
            batchedRects = 0;
        }
    }

    private void updateScreenOffset() {
        view.getLocationOnScreen(screenOffset);
    }

    private int orientation() {
        return view.getResources().getConfiguration().orientation;
    }
}
