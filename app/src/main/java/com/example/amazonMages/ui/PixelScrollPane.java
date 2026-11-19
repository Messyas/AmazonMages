package com.example.amazonMages.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;

/**
 * Reusable zero-allocation pixel-art scroll pane.
 * Handles touch gestures (drag scrolling with touch-slop), viewport clipping, and
 * renders a crisp wooden/gold pixel scrollbar track and thumb.
 */
public final class PixelScrollPane {
    private static final float TOUCH_SLOP = 12f;

    private float scrollY;
    private float maxScroll;
    private float lastTouchY;
    private float downTouchY;
    private float downTouchX;
    private boolean isDragging;
    private final RectF trackRect = new RectF();
    private final RectF thumbRect = new RectF();
    private final Paint paint = new Paint();

    public PixelScrollPane() {
        paint.setAntiAlias(false);
        paint.setDither(false);
    }

    public float getScrollY() {
        return scrollY;
    }

    public void setScrollY(float scrollY) {
        this.scrollY = Math.max(0f, Math.min(maxScroll, scrollY));
    }

    public boolean isDragging() {
        return isDragging;
    }

    public void reset() {
        scrollY = 0f;
        isDragging = false;
    }

    /**
     * Handles touch events inside or targeting this scroll viewport.
     * Returns true if the event was consumed by scrolling or dragging.
     */
    public boolean onTouchEvent(MotionEvent event, RectF viewport, float contentHeight) {
        if (viewport == null || contentHeight <= viewport.height()) {
            isDragging = false;
            return false;
        }
        maxScroll = Math.max(0f, contentHeight - viewport.height());
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (viewport.contains(x, y)) {
                    downTouchX = x;
                    downTouchY = y;
                    lastTouchY = y;
                    isDragging = false;
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                if (!viewport.contains(downTouchX, downTouchY)) return false;
                float dy = y - lastTouchY;
                lastTouchY = y;
                if (!isDragging) {
                    float totalDy = Math.abs(y - downTouchY);
                    float totalDx = Math.abs(x - downTouchX);
                    if (totalDy > TOUCH_SLOP && totalDy > totalDx) {
                        isDragging = true;
                    }
                }
                if (isDragging) {
                    scrollY = Math.max(0f, Math.min(maxScroll, scrollY - dy));
                    return true;
                }
                return false;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                boolean wasDrag = isDragging;
                isDragging = false;
                downTouchX = -1;
                downTouchY = -1;
                return wasDrag;

            default:
                return false;
        }
    }

    /** Begins clipped and translated rendering for child content. */
    public void begin(Canvas canvas, RectF viewport) {
        canvas.save();
        canvas.clipRect(viewport);
        canvas.translate(0, -scrollY);
    }

    /** Restores canvas clip and matrix after child content. */
    public void end(Canvas canvas) {
        canvas.restore();
    }

    /** Draws the pixel-art wooden scrollbar track and gold thumb on the right edge of viewport. */
    public void drawScrollbar(Canvas canvas, PixelUiKit ui, RectF viewport, float contentHeight) {
        if (viewport == null || contentHeight <= viewport.height()) return;
        maxScroll = Math.max(0f, contentHeight - viewport.height());

        float trackWidth = Math.max(6f, ui.scale() * 2.2f);
        float trackRight = viewport.right - 2f;
        float trackLeft = trackRight - trackWidth;
        float trackTop = viewport.top + 4f;
        float trackBottom = viewport.bottom - 4f;
        trackRect.set(trackLeft, trackTop, trackRight, trackBottom);

        // Dark forest wooden track
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF0B1A12);
        canvas.drawRect(trackRect, paint);
        paint.setColor(0xFF1D3524);
        canvas.drawRect(trackLeft + 1f, trackTop + 1f, trackRight - 1f, trackBottom - 1f, paint);

        // Gold thumb sized proportionally to visible ratio
        float visibleRatio = viewport.height() / contentHeight;
        float trackH = trackRect.height();
        float thumbH = Math.max(16f * ui.scale(), trackH * visibleRatio);
        float scrollRatio = maxScroll > 0 ? (scrollY / maxScroll) : 0f;
        float thumbTop = trackTop + (trackH - thumbH) * scrollRatio;
        thumbRect.set(trackLeft + 1f, thumbTop, trackRight - 1f, thumbTop + thumbH);

        paint.setColor(0xFFC99A2E);
        canvas.drawRect(thumbRect, paint);
        paint.setColor(0xFFF2D36B);
        canvas.drawRect(thumbRect.left + 1f, thumbRect.top + 1f, thumbRect.right - 1f, thumbRect.bottom - 1f, paint);
    }
}
