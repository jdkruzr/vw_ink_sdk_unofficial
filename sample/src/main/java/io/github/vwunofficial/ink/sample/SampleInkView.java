package io.github.vwunofficial.ink.sample;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.View;

import io.github.vwunofficial.ink.ViwoodsBitmapProvider;
import io.github.vwunofficial.ink.ViwoodsInkConfig;
import io.github.vwunofficial.ink.ViwoodsInkController;
import io.github.vwunofficial.ink.ViwoodsInkEvent;
import io.github.vwunofficial.ink.ViwoodsInkLogger;
import io.github.vwunofficial.ink.ViwoodsInkRenderer;
import io.github.vwunofficial.ink.ViwoodsInkStartResult;

final class SampleInkView extends View implements ViwoodsBitmapProvider, ViwoodsInkRenderer {
    private static final int DIRTY_PAD = 12;

    private final Paint bitmapPaint = new Paint(Paint.DITHER_FLAG);
    private final Paint penPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final Rect dirty = new Rect();
    private final ViwoodsInkController controller;
    private Bitmap bitmap;
    private Canvas bitmapCanvas;
    private boolean strokeActive;
    private float lastX;
    private float lastY;
    private String status = "starting";

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
        controller = new ViwoodsInkController(
                this,
                this,
                this,
                ViwoodsInkConfig.defaults(),
                new ViwoodsInkLogger() {
                    @Override
                    public void log(String message) {
                        android.util.Log.i("ViwoodsInkSample", message);
                    }
                });
    }

    void startViwoodsInk() {
        ViwoodsInkStartResult result = controller.startWithResult();
        status = result.started ? "Viwoods fast ink active" : result.status + ": " + result.detail;
        invalidate();
    }

    void stopViwoodsInk() {
        controller.stop();
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
        penPaint.setStrokeWidth(3.5f * Math.max(0.75f, Math.min(1.8f, event.pressure + 0.35f)));
        if (event.action == MotionEvent.ACTION_DOWN) {
            strokeActive = true;
            path.reset();
            path.moveTo(event.x, event.y);
            lastX = event.x;
            lastY = event.y;
            setDirty(event.x, event.y, event.x, event.y);
        } else if (event.action == MotionEvent.ACTION_MOVE) {
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
        } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
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
        canvas.drawText(status, 24, 48, textPaint);
    }

    private void setDirty(float x1, float y1, float x2, float y2) {
        int left = Math.max(0, (int) Math.floor(Math.min(x1, x2)) - DIRTY_PAD);
        int top = Math.max(0, (int) Math.floor(Math.min(y1, y2)) - DIRTY_PAD);
        int right = Math.min(getWidth(), (int) Math.ceil(Math.max(x1, x2)) + DIRTY_PAD);
        int bottom = Math.min(getHeight(), (int) Math.ceil(Math.max(y1, y2)) + DIRTY_PAD);
        if (right <= left || bottom <= top) {
            dirty.setEmpty();
        } else {
            dirty.set(left, top, right, bottom);
        }
    }
}
