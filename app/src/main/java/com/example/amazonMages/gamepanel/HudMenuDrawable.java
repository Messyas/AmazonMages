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

/** Round pixel-art pause/menu button matching ui_button_round + ui_icons[7] (pause). */
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
        buttons = resources.bitmap(R.drawable.ui_button_round, false);
        icons = resources.bitmap(R.drawable.ui_icons, false);
        scale = Math.max(2, Math.min(4, Math.round(context.getResources().getDisplayMetrics().density)));
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        paint.setDither(false);
    }

    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;
        float size = Math.min(Math.min(bounds.width(), bounds.height()), 32f * scale);
        float left = bounds.exactCenterX() - size * 0.5f;
        float top = bounds.exactCenterY() - size * 0.5f;
        int frame = !enabled ? 2 : pressed ? 1 : 0;
        if (buttons != null) {
            source.set(frame * 32, 0, (frame + 1) * 32, 32);
            target.set(left, top, left + size, top + size);
            canvas.drawBitmap(buttons, source, target, paint);
        }
        if (icons != null) {
            source.set(7 * 16, 0, 8 * 16, 16); // pause bars icon in ui_icons
            float iconSize = size * 0.5f;
            float shiftY = pressed ? scale : 0f;
            target.set(bounds.exactCenterX() - iconSize * 0.5f,
                bounds.exactCenterY() - iconSize * 0.5f + shiftY,
                bounds.exactCenterX() + iconSize * 0.5f,
                bounds.exactCenterY() + iconSize * 0.5f + shiftY);
            paint.setAlpha(enabled ? 255 : 130);
            canvas.drawBitmap(icons, source, target, paint);
            paint.setAlpha(255);
        }
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
