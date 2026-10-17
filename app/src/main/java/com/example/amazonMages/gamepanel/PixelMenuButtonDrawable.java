package com.example.amazonMages.gamepanel;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

/** Stateful, hard-edged moss-stone button used by menu and dialog controls. */
public final class PixelMenuButtonDrawable extends Drawable {
    private final Paint paint = new Paint();
    private final int unit;
    private boolean pressed;
    private boolean enabled = true;
    private boolean selected;

    public PixelMenuButtonDrawable(float density) {
        unit = Math.max(1, Math.round(density));
        paint.setAntiAlias(false);
        paint.setDither(false);
    }

    @Override public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (b.width() <= unit * 4 || b.height() <= unit * 4) return;
        int offset = pressed && enabled ? unit : 0;
        float l = b.left + unit * 2;
        float t = b.top + unit * 2 + offset;
        float r = b.right - unit * 2;
        float bottom = b.bottom - unit * 2;

        color(canvas, enabled ? 0xFF1A2E23 : 0xFF253027);
        canvas.drawRect(l, t + unit, r, bottom, paint);
        color(canvas, enabled ? 0xFF2B1A12 : 0xFF302820);
        canvas.drawRect(l + unit, t, r - unit, bottom, paint);

        color(canvas, enabled ? (selected ? 0xFF96752E : 0xFF4A5E3C) : 0xFF3B4539);
        canvas.drawRect(l + unit * 2, t + unit * 2, r - unit * 2, bottom - unit * 2, paint);

        color(canvas, enabled ? (pressed ? 0xFF6F8A52 : 0xFFA8C27A) : 0xFF66715D);
        canvas.drawRect(l + unit * 3, t + unit, r - unit * 3, t + unit * 2, paint);
        canvas.drawRect(l + unit, t + unit * 3, l + unit * 2, bottom - unit * 3, paint);
        color(canvas, enabled ? 0xFF1E3C29 : 0xFF303B31);
        canvas.drawRect(l + unit * 2, bottom - unit * 2, r - unit * 2, bottom - unit, paint);
        canvas.drawRect(r - unit * 2, t + unit * 3, r - unit, bottom - unit * 3, paint);

        int rivet = enabled ? (selected ? 0xFFF2D36B : 0xFF9C7939) : 0xFF68715D;
        color(canvas, rivet);
        canvas.drawRect(l + unit * 2, t + unit * 2, l + unit * 3, t + unit * 3, paint);
        canvas.drawRect(r - unit * 3, t + unit * 2, r - unit * 2, t + unit * 3, paint);
    }

    private void color(Canvas canvas, int color) {
        paint.setColor(color);
    }

    @Override protected boolean onStateChange(int[] state) {
        boolean nextPressed = false, nextEnabled = true, nextSelected = false;
        for (int value : state) {
            if (value == android.R.attr.state_pressed) nextPressed = true;
            else if (value == -android.R.attr.state_enabled) nextEnabled = false;
            else if (value == android.R.attr.state_selected || value == android.R.attr.state_focused)
                nextSelected = true;
        }
        if (pressed == nextPressed && enabled == nextEnabled && selected == nextSelected) return false;
        pressed = nextPressed;
        enabled = nextEnabled;
        selected = nextSelected;
        invalidateSelf();
        return true;
    }

    @Override public boolean isStateful() { return true; }
    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter colorFilter) { paint.setColorFilter(colorFilter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
