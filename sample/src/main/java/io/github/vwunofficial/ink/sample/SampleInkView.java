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
    private static final int DIRTY_PAD_PX = 24;

    private final Paint bitmapPaint = new Paint(Paint.DITHER_FLAG);
    private final Paint penPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint accentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
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
    private Tool tool = Tool.STEEL;
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
        accentPaint.setStyle(Paint.Style.STROKE);
        accentPaint.setStrokeCap(Paint.Cap.ROUND);
        accentPaint.setStrokeJoin(Paint.Join.ROUND);
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

    String cycleTool() {
        Tool[] tools = Tool.values();
        tool = tools[(tool.ordinal() + 1) % tools.length];
        status = "tool " + tool.label;
        invalidate();
        return tool.label;
    }

    String toolLabel() {
        return tool.label;
    }

    private ViwoodsInkController createController() {
        return new ViwoodsInkController(
                this,
                this,
                this,
                ViwoodsInkConfig.builder()
                        .renderBatchSize(renderBatchSize)
                        .dirtyRectPaddingPx(DIRTY_PAD_PX)
                        .directInputCallbacks(true)
                        .invalidateView(false)
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
                drawSegment(lastX, lastY, event.x, event.y, event.pressure);
                setDirty(lastX, lastY, event.x, event.y);
                lastX = event.x;
                lastY = event.y;
            }
        } else if (event.isUpOrCancel()) {
            if (strokeActive) {
                drawSegment(lastX, lastY, event.x, event.y, event.pressure);
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
        y += lineHeight;
        canvas.drawText("tool " + tool.label, x, y, textPaint);
    }

    private void drawSegment(float x1, float y1, float x2, float y2, float pressure) {
        configurePaints(pressure);
        path.reset();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        if (tool == Tool.CHARCOAL_PENCIL || tool == Tool.PENCIL2B || tool == Tool.PENCIL4B
                || tool == Tool.PENCIL6B || tool == Tool.PENCIL8B) {
            bitmapCanvas.drawPath(path, accentPaint);
        }
        bitmapCanvas.drawPath(path, penPaint);
        if (tool == Tool.ART || tool == Tool.ART_THINKERS || tool == Tool.PAINTBRUSH) {
            path.reset();
            path.moveTo(x1 + 1.7f, y1 - 1.7f);
            path.lineTo(x2 + 1.7f, y2 - 1.7f);
            bitmapCanvas.drawPath(path, accentPaint);
        }
    }

    private void configurePaints(float pressure) {
        float p = Math.max(0.65f, Math.min(1.9f, pressure + 0.35f));
        penPaint.setColor(Color.BLACK);
        penPaint.setAlpha(255);
        penPaint.setStrokeCap(Paint.Cap.ROUND);
        penPaint.setStrokeJoin(Paint.Join.ROUND);
        accentPaint.setColor(Color.BLACK);
        accentPaint.setAlpha(255);
        accentPaint.setStrokeCap(Paint.Cap.ROUND);
        accentPaint.setStrokeJoin(Paint.Join.ROUND);

        switch (tool) {
            case TECHNICAL:
                penPaint.setStrokeWidth(2.2f);
                break;
            case BALL:
                penPaint.setStrokeWidth(3.0f * p);
                break;
            case MARK:
                penPaint.setStrokeWidth(8.0f);
                penPaint.setAlpha(210);
                break;
            case HIGHLIGHTER:
                penPaint.setColor(Color.rgb(170, 170, 170));
                penPaint.setAlpha(130);
                penPaint.setStrokeWidth(18.0f);
                break;
            case PENCIL:
            case PENCIL2B:
            case PENCIL4B:
            case PENCIL6B:
            case PENCIL8B:
            case CHARCOAL_PENCIL:
                penPaint.setStrokeWidth(pencilWidth(tool) * p);
                penPaint.setAlpha(pencilAlpha(tool));
                accentPaint.setColor(Color.rgb(95, 95, 95));
                accentPaint.setAlpha(75);
                accentPaint.setStrokeWidth(pencilWidth(tool) * p + 2.0f);
                break;
            case PAINTBRUSH:
                penPaint.setStrokeWidth(10.0f * p);
                accentPaint.setAlpha(95);
                accentPaint.setStrokeWidth(4.0f * p);
                break;
            case ART:
            case ART_THINKERS:
                penPaint.setStrokeCap(Paint.Cap.SQUARE);
                penPaint.setStrokeWidth(7.0f * p);
                accentPaint.setStrokeCap(Paint.Cap.SQUARE);
                accentPaint.setAlpha(160);
                accentPaint.setStrokeWidth(2.5f * p);
                break;
            case SMUDGE:
                penPaint.setColor(Color.rgb(130, 130, 130));
                penPaint.setAlpha(105);
                penPaint.setStrokeWidth(15.0f);
                break;
            case STEEL:
            default:
                penPaint.setStrokeWidth(3.5f * p);
                break;
        }
    }

    private static float pencilWidth(Tool tool) {
        switch (tool) {
            case PENCIL8B:
            case CHARCOAL_PENCIL:
                return 8.0f;
            case PENCIL6B:
                return 6.5f;
            case PENCIL4B:
                return 5.0f;
            case PENCIL2B:
                return 3.8f;
            default:
                return 3.2f;
        }
    }

    private static int pencilAlpha(Tool tool) {
        switch (tool) {
            case PENCIL8B:
            case CHARCOAL_PENCIL:
                return 230;
            case PENCIL6B:
                return 205;
            case PENCIL4B:
                return 180;
            case PENCIL2B:
                return 155;
            default:
                return 140;
        }
    }

    private static String formatMillis(long nanos) {
        if (nanos < 0L) {
            return "-";
        }
        return String.format(java.util.Locale.US, "%.3fms", nanos / 1_000_000.0);
    }

    private enum Tool {
        STEEL("STEEL"),
        BALL("BALL"),
        TECHNICAL("TECHNICAL"),
        MARK("MARK"),
        HIGHLIGHTER("HIGHLIGHTER"),
        PENCIL("PENCIL"),
        PENCIL2B("PENCIL2B"),
        PENCIL4B("PENCIL4B"),
        PENCIL6B("PENCIL6B"),
        PENCIL8B("PENCIL8B"),
        CHARCOAL_PENCIL("CHARCOAL"),
        PAINTBRUSH("PAINTBRUSH"),
        ART("ART"),
        ART_THINKERS("THINKERS"),
        SMUDGE("SMUDGE");

        final String label;

        Tool(String label) {
            this.label = label;
        }
    }
}
