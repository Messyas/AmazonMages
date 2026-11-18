package com.example.amazonMages.gamepanel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import com.example.amazonMages.resources.ResourceManager;
import com.example.androidstudio2dgamedevelopment.R;

/** Three real button frames with opaque 9-slice caps and integer-tiled border edges. */
public final class AtlasMenuButtonDrawable extends Drawable {
    private final Bitmap atlas;
    private final Paint paint = new Paint();
    private final Rect source = new Rect();
    private final Rect target = new Rect();
    private final int[] sourceX = {1, 10, 22, 31};
    private final int[] sourceY = {2, 6, 13, 17};
    private final int[] targetX = new int[4];
    private final int[] targetY = new int[4];
    private int frame;
    private int pixelScale = 2;
    private boolean focused;

    public AtlasMenuButtonDrawable(Context context) {
        atlas = ResourceManager.shared(context).bitmap(R.drawable.ui_button, false);
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        paint.setDither(false);
    }

    public void setPixelScale(int scale) {
        pixelScale = Math.max(1, scale);
        invalidateSelf();
    }

    @Override public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (atlas == null || b.isEmpty()) return;
        int capX = Math.min(9 * pixelScale, b.width() / 4);
        int capY = Math.min(4 * pixelScale, b.height() / 4);
        targetX[0] = b.left; targetX[1] = b.left + capX; targetX[2] = b.right - capX; targetX[3] = b.right;
        targetY[0] = b.top; targetY[1] = b.top + capY; targetY[2] = b.bottom - capY; targetY[3] = b.bottom;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int dl = targetX[col], dt = targetY[row], dr = targetX[col + 1], db = targetY[row + 1];
                if (dr <= dl || db <= dt) continue;
                int sl = frame * 32 + sourceX[col], st = sourceY[row];
                int sr = frame * 32 + sourceX[col + 1], sb = sourceY[row + 1];
                if (col == 1 && row != 1) {
                    int sw = sr - sl;
                    int step = Math.max(4, sw * pixelScale);
                    for (int x = dl; x < dr; x += step) {
                        int segW = Math.min(step, dr - x);
                        int srcStop = sl + Math.max(1, Math.round(sw * (segW / (float) step)));
                        source.set(sl, st, Math.min(sr, srcStop), sb);
                        target.set(x, dt, x + segW, db);
                        canvas.drawBitmap(atlas, source, target, paint);
                    }
                } else {
                    source.set(sl, st, sr, sb);
                    target.set(dl, dt, dr, db);
                    canvas.drawBitmap(atlas, source, target, paint);
                }
            }
        }
        if (focused && frame != 2) {
            int color = paint.getColor();
            paint.setColor(0xFFF2D36B);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(pixelScale);
            canvas.drawRect(b.left + pixelScale, b.top + pixelScale,
                b.right - pixelScale, b.bottom - pixelScale, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(color);
        }
    }

    @Override protected boolean onStateChange(int[] states) {
        boolean enabled = false, pressed = false, nextFocused = false;
        for (int state : states) {
            if (state == android.R.attr.state_enabled) enabled = true;
            if (state == android.R.attr.state_pressed) pressed = true;
            if (state == android.R.attr.state_focused || state == android.R.attr.state_selected)
                nextFocused = true;
        }
        int nextFrame = !enabled ? 2 : pressed ? 1 : 0;
        if (nextFrame == frame && nextFocused == focused) return false;
        frame = nextFrame;
        focused = nextFocused;
        invalidateSelf();
        return true;
    }

    @Override public boolean isStateful() { return true; }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
