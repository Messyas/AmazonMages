package com.example.amazonMages.gamepanel;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Pixel-edged forest panel with wood frame, moss-green paper, and tiny leaf corners. */
public final class WoodlandPanelDrawable extends Drawable {
    private final Paint paint = new Paint();
    private final int p;

    public WoodlandPanelDrawable(float density) {
        p = Math.max(1, Math.round(density));
        paint.setAntiAlias(false);
        paint.setDither(false);
    }

    @Override public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.width() < p * 8 || b.height() < p * 8) return;
        float l = b.left, t = b.top, r = b.right, bottom = b.bottom;
        rect(canvas, l, t, r, bottom, 0xFF2B1A12);
        rect(canvas, l + p, t + p, r - p, bottom - p, 0xFF8F6238);
        rect(canvas, l + p * 2, t + p * 2, r - p * 2, bottom - p * 2, 0xFF4A2E1F);
        rect(canvas, l + p * 4, t + p * 4, r - p * 4, bottom - p * 4, 0xFF10241A);
        rect(canvas, l + p * 5, t + p * 5, r - p * 5, t + p * 6, 0xFF1F4228);
        rect(canvas, l + p * 5, t + p * 5, l + p * 6, bottom - p * 5, 0xFF1F4228);
        rect(canvas, l + p * 5, bottom - p * 6, r - p * 5, bottom - p * 5, 0xFF09180F);
        rect(canvas, r - p * 6, t + p * 5, r - p * 5, bottom - p * 5, 0xFF09180F);

        float[][] corners = {
            {l + p * 3, t + p * 3}, {r - p * 5, t + p * 3},
            {l + p * 3, bottom - p * 5}, {r - p * 5, bottom - p * 5}
        };
        for (int i = 0; i < corners.length; i++) {
            float x = corners[i][0], y = corners[i][1];
            rect(canvas, x, y, x + p * 2, y + p * 2, 0xFFC99A2E);
            rect(canvas, x + p, y + p, x + p * 2, y + p * 2, 0xFFFFF1B0);
        }
        leaf(canvas, l + p * 7, t + p * 2, false);
        leaf(canvas, r - p * 8, t + p * 2, true);
        leaf(canvas, l + p * 7, bottom - p * 4, true);
        leaf(canvas, r - p * 8, bottom - p * 4, false);
    }

    private void leaf(Canvas canvas, float x, float y, boolean flip) {
        float d = flip ? -p : p;
        rect(canvas, x, y + p, x + d * 2, y + p * 2, 0xFF1E6436);
        rect(canvas, x + d, y, x + d * 3, y + p, 0xFF3DC45E);
        rect(canvas, x + d * 2, y + p, x + d * 3, y + p * 2, 0xFF9BF06A);
    }

    private void rect(Canvas canvas, float l, float t, float r, float b, int color) {
        if (r < l) { float swap = l; l = r; r = swap; }
        paint.setColor(color);
        canvas.drawRect(l, t, r, b, paint);
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter colorFilter) { paint.setColorFilter(colorFilter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
