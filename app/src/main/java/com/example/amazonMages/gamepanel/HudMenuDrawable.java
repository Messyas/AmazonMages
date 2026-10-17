package com.example.amazonMages.gamepanel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import com.example.androidstudio2dgamedevelopment.R;
import com.example.amazonMages.resources.ResourceManager;

/** Pixel-art pause/menu button for the Android overlay control. */
public final class HudMenuDrawable extends Drawable {
    private final Bitmap buttons;
    private final Bitmap icons;
    private final Paint paint = new Paint();
    private final Rect source = new Rect();
    private final RectF target = new RectF();
    private final int scale;
    private boolean pressed;
    private boolean enabled = true;

    public HudMenuDrawable(Context context) {
        ResourceManager resources = ResourceManager.shared(context);
        buttons = resources.bitmap(R.drawable.hud_button, false);
        icons = resources.bitmap(R.drawable.hud_icons, false);
        scale = Math.max(2, Math.min(4, Math.round(context.getResources().getDisplayMetrics().density)));
        paint.setFilterBitmap(false);
        paint.setDither(false);
    }

    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        int size = 32 * scale;
        float left = bounds.exactCenterX() - size / 2f;
        float top = bounds.exactCenterY() - size / 2f;
        int frame = !enabled ? 2 : pressed ? 1 : 0;
        source.set(frame * 32, 0, (frame + 1) * 32, 32);
        target.set(left, top, left + size, top + size);
        canvas.drawBitmap(buttons, source, target, paint);
        source.set(10 * 16, 0, 11 * 16, 16);
        float iconSize = 16 * scale;
        target.set(bounds.exactCenterX() - iconSize / 2f, bounds.exactCenterY() - iconSize / 2f,
            bounds.exactCenterX() + iconSize / 2f, bounds.exactCenterY() + iconSize / 2f);
        paint.setAlpha(enabled ? 255 : 130);
        canvas.drawBitmap(icons, source, target, paint);
        paint.setAlpha(255);
    }

    @Override protected boolean onStateChange(int[] state) {
        boolean nextPressed = false, nextEnabled = true;
        for (int value : state) {
            if (value == android.R.attr.state_pressed) nextPressed = true;
            else if (value == -android.R.attr.state_enabled) nextEnabled = false;
        }
        if (pressed == nextPressed && enabled == nextEnabled) return false;
        pressed = nextPressed;
        enabled = nextEnabled;
        invalidateSelf();
        return true;
    }

    @Override public boolean isStateful() { return true; }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(android.graphics.ColorFilter colorFilter) { paint.setColorFilter(colorFilter); invalidateSelf(); }
    @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
}
