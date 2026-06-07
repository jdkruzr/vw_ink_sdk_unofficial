package io.github.vwunofficial.ink.sample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.View;

import io.github.vwunofficial.ink.ViwoodsBitmapProvider;
import io.github.vwunofficial.ink.ViwoodsInkAction;
import io.github.vwunofficial.ink.ViwoodsInkConfig;
import io.github.vwunofficial.ink.ViwoodsInkController;
import io.github.vwunofficial.ink.ViwoodsInkEvent;
import io.github.vwunofficial.ink.ViwoodsInkListener;
import io.github.vwunofficial.ink.ViwoodsInkLogger;
import io.github.vwunofficial.ink.ViwoodsInkRenderResult;
import io.github.vwunofficial.ink.ViwoodsInkRenderer;
import io.github.vwunofficial.ink.ViwoodsInkStartResult;

final class SampleInkView extends View implements ViwoodsBitmapProvider, ViwoodsInkRenderer {
    private static final int DIRTY_PAD_PX = 12;

    private final Paint bitmapPaint = new Paint(Paint.DITHER_FLAG);
    private final Paint penPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Rect dirty = new Rect();
    private final Rect fullViewRect = new Rect();
    private ViwoodsInkController controller;
    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private boolean strokeActive;
    private boolean inkStarted;
    private int renderBatchSize = 2;
    private float lastX;
    private float lastY;
    private String status = "starting";
    private int eventCount;
    private int strokeCount;
    private int renderCount;
    private int renderFailureCount;
    private long lastRenderNanos = -1L;
    private String lastRenderRect = "";

    SampleInkView(Context context) {
        super(context);
        setBackgroundColor(Color.WHITE);
        penPaint.setColor(Color.BLACK);
        penPaint.setStrokeWidth(4f);
        penPaint.setStyle(Paint.Style.STROKE);
        penPaint.setStrokeCap(Paint.Cap.ROUND);
        penPaint.setStrokeJoin(Paint.Join.ROUND);
        textPaint.setColor(Color.rgb(32, 32, 32));
        textPaint.setTextSize(30f);
        controller = createController();
    }

    void startViwoodsInk() {
        ViwoodsInkStartResult result = controller.startWithResult();
        inkStarted = controller.isRunning();
        status = result.started ? "Viwoods fast ink active" : result.status + ": " + result.detail;
        invalidate();
    }

    void stopViwoodsInk() {
        controller.stop();
        inkStarted = false;
    }

    void clearInk() {
        if (bitmapCanvas != null) {
            bitmapCanvas.drawColor(Color.WHITE);
        }
        strokeActive = false;
        path.reset();
        fullViewRect.set(0, 0, getWidth(), getHeight());
        if (controller.isRunning() && !fullViewRect.isEmpty()) {
            controller.renderNow(fullViewRect);
        }
        invalidate();
    }

    void resetStats() {
        eventCount = 0;
        strokeCount = 0;
        renderCount = 0;
        renderFailureCount = 0;
        lastRenderNanos = -1L;
        lastRenderRect = "";
        status = controller.isRunning() ? "Viwoods fast ink active" : "stopped";
        invalidate();
    }

    int cycleBatchSize() {
        if (renderBatchSize == 1) {
            renderBatchSize = 2;
        } else if (renderBatchSize == 2) {
            renderBatchSize = 4;
        } else {
            renderBatchSize = 1;
        }
        boolean restart = controller.isRunning() || inkStarted;
        controller.stop();
        inkStarted = false;
        controller = createController();
        if (restart) {
            startViwoodsInk();
        } else {
            status = "batch " + renderBatchSize;
            invalidate();
        }
        return renderBatchSize;
    }

    int renderBatchSize() {
        return renderBatchSize;
    }

    private ViwoodsInkController createController() {
        return new ViwoodsInkController(
                this,
                this,
                this,
                ViwoodsInkConfig.builder()
                        .renderBatchSize(renderBatchSize)
                        .dirtyRectPaddingPx(DIRTY_PAD_PX)
                        .listener(new ViwoodsInkListener() {
                            @Override
                            public void onStrokeStart(ViwoodsInkEvent event) {
                                strokeCount++;
                            }

                            @Override
                            public void onRenderResult(ViwoodsInkRenderResult result) {
                                renderCount++;
                                lastRenderNanos = result.elapsedNanos;
                                lastRenderRect = result.screenRect.toShortString();
                            }

                            @Override
                            public void onRenderFailure(ViwoodsInkRenderResult result) {
                                renderFailureCount++;
                                status = "render failed: " + result.detail;
                            }
                        })
                        .build(),
                new ViwoodsInkLogger() {
                    @Override
                    public void log(String message) {
                        android.util.Log.i("ViwoodsInkSample", message);
                    }
                });
    }

    @Override
    public Bitmap getInkBitmap() {
        return bitmap;
    }

    @Override
    public Rect onInkEvent(ViwoodsInkEvent event) {
        if (bitmapCanvas == null) {
            return null;
        }
        eventCount++;
        penPaint.setStrokeWidth(3.5f * Math.max(0.75f, Math.min(1.8f, event.pressure + 0.35f)));
        if (event.actionType == ViwoodsInkAction.DOWN) {
            strokeActive = true;
            path.reset();
            path.moveTo(event.x, event.y);
            lastX = event.x;
            lastY = event.y;
            setDirty(event.x, event.y, event.x, event.y);
        } else if (event.actionType == ViwoodsInkAction.MOVE) {
            if (!strokeActive) {
                strokeActive = true;
                path.reset();
                path.moveTo(event.x, event.y);
                lastX = event.x;
                lastY = event.y;
                setDirty(event.x, event.y, event.x, event.y);
            } else {
                path.reset();
                path.moveTo(lastX, lastY);
                path.lineTo(event.x, event.y);
                bitmapCanvas.drawPath(path, penPaint);
                setDirty(lastX, lastY, event.x, event.y);
                lastX = event.x;
                lastY = event.y;
            }
        } else if (event.isUpOrCancel()) {
            if (strokeActive) {
                path.reset();
                path.moveTo(lastX, lastY);
                path.lineTo(event.x, event.y);
                bitmapCanvas.drawPath(path, penPaint);
                setDirty(lastX, lastY, event.x, event.y);
            } else {
                setDirty(event.x, event.y, event.x, event.y);
            }
            strokeActive = false;
            lastX = event.x;
            lastY = event.y;
        } else {
            dirty.setEmpty();
        }
        return dirty;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        if (w <= 0 || h <= 0) {
            return;
        }
        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmapCanvas = new Canvas(bitmap);
        bitmapCanvas.drawColor(Color.WHITE);
        controller.refreshBitmap();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap != null) {
            canvas.drawBitmap(bitmap, 0, 0, bitmapPaint);
        }
        drawOverlay(canvas);
    }

    private void setDirty(float x1, float y1, float x2, float y2) {
        int left = Math.max(0, (int) Math.floor(Math.min(x1, x2)));
        int top = Math.max(0, (int) Math.floor(Math.min(y1, y2)));
        int right = Math.min(getWidth(), (int) Math.ceil(Math.max(x1, x2)));
        int bottom = Math.min(getHeight(), (int) Math.ceil(Math.max(y1, y2)));
        if (right == left && right < getWidth()) {
            right++;
        }
        if (bottom == top && bottom < getHeight()) {
            bottom++;
        }
        if (right <= left || bottom <= top) {
            dirty.setEmpty();
        } else {
            dirty.set(left, top, right, bottom);
        }
    }

    private void drawOverlay(Canvas canvas) {
        float x = 24f;
        float y = 48f;
        float lineHeight = 34f;
        canvas.drawText(status, x, y, textPaint);
        y += lineHeight;
        canvas.drawText("events " + eventCount + " strokes " + strokeCount
                + " renders " + renderCount + " failures " + renderFailureCount, x, y, textPaint);
        y += lineHeight;
        canvas.drawText("batch " + renderBatchSize + " pad " + DIRTY_PAD_PX + " last "
                + formatMillis(lastRenderNanos) + " " + lastRenderRect, x, y, textPaint);
    }

    private static String formatMillis(long nanos) {
        if (nanos < 0L) {
            return "-";
        }
        return String.format(java.util.Locale.US, "%.3fms", nanos / 1_000_000.0);
    }
}
