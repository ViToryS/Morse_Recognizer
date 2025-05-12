package com.example.morse_recognizer.ui.components;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.morse_recognizer.R;

public class OverlayView extends View {
    private final Rect selectionRect = new Rect();
    private final Paint paint = new Paint();

    private static final int TOUCH_AREA_SIZE = 40;
    private static final int MIN_SIZE = 100;

    private enum Mode { NONE, DRAG, RESIZE }
    private Mode currentMode = Mode.NONE;
    private float lastX, lastY;

    @Override
    protected void onSizeChanged(int w, int h, int oldWidth, int olfHeight) {
        super.onSizeChanged(w, h, oldWidth, olfHeight);

        int rectWidth = (int) (w * 0.6);

        int left = (w - rectWidth) / 2;
        int top = (h - rectWidth) / 2;
        int right = left + rectWidth;
        int bottom = top + rectWidth;

        selectionRect.set(left, top, right, bottom);

    }

    public OverlayView(Context context) {
        super(context);
        init();
    }

    public OverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint.setColor(ContextCompat.getColor(getContext(), R.color.purple_500));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
    }

    public Rect getSelectionRect() {
        return new Rect(selectionRect);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        Paint backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#88000000"));

        canvas.drawRect(0, 0, getWidth(), selectionRect.top, backgroundPaint);
        canvas.drawRect(0, selectionRect.bottom, getWidth(), getHeight(), backgroundPaint);
        canvas.drawRect(0, selectionRect.top, selectionRect.left, selectionRect.bottom, backgroundPaint);
        canvas.drawRect(selectionRect.right, selectionRect.top, getWidth(), selectionRect.bottom, backgroundPaint);

        canvas.drawRect(selectionRect, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isInCorner(x, y)) {
                    currentMode = Mode.RESIZE;
                } else if (selectionRect.contains((int) x, (int) y)) {
                    currentMode = Mode.DRAG;
                }
                lastX = x;
                lastY = y;
                break;

            case MotionEvent.ACTION_MOVE:
                float dx = x - lastX;
                float dy = y - lastY;

                if (currentMode == Mode.DRAG) {
                    moveRect(dx, dy);
                } else if (currentMode == Mode.RESIZE) {
                    resizeRect(dx, dy);
                }

                lastX = x;
                lastY = y;
                invalidate();
                break;

            case MotionEvent.ACTION_UP:
                currentMode = Mode.NONE;
                break;
        }

        return true;
    }

    private boolean isInCorner(float x, float y) {
        return (Math.abs(x - selectionRect.right) < TOUCH_AREA_SIZE &&
                Math.abs(y - selectionRect.bottom) < TOUCH_AREA_SIZE);
    }

    private void moveRect(float dx, float dy) {
        selectionRect.offset((int) dx, (int) dy);

        if (selectionRect.left < 0) selectionRect.offset(-selectionRect.left, 0);
        if (selectionRect.top < 0) selectionRect.offset(0, -selectionRect.top);
        if (selectionRect.right > getWidth()) selectionRect.offset(getWidth() - selectionRect.right, 0);
        if (selectionRect.bottom > getHeight()) selectionRect.offset(0, getHeight() - selectionRect.bottom);
    }

    private void resizeRect(float dx, float dy) {
        Log.d("Resize", "До: " + selectionRect.toShortString() + ", dx: " + dx + ", dy: " + dy);
        selectionRect.right += (int) dx;
        selectionRect.bottom += (int) dy;

        if (selectionRect.width() < MIN_SIZE) selectionRect.right = selectionRect.left + MIN_SIZE;
        if (selectionRect.height() < MIN_SIZE) selectionRect.bottom = selectionRect.top + MIN_SIZE;
        if (selectionRect.right > getWidth()) selectionRect.right = getWidth();
        if (selectionRect.bottom > getHeight()) selectionRect.bottom = getHeight();
        Log.d("Resize", "После: " + selectionRect.toShortString());
    }
}
