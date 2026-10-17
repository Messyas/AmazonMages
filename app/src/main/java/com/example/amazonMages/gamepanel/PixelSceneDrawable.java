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

/** Expands the 400x180 title background by whole pixels, cropping only at the screen edges. */
public final class PixelSceneDrawable extends Drawable {
    private final Bitmap scene;
    private final Paint paint = new Paint();
    private final Rect target = new Rect();

    public PixelSceneDrawable(Context context) {
        scene = ResourceManager.shared(context).bitmap(R.drawable.bg_menu, false);
        paint.setFilterBitmap(false);
        paint.setAntiAlias(false);
        paint.setDither(false);
    }

    @Override public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;
        int scale = Math.max(1, (int) Math.ceil(Math.max(
            bounds.width() / (double) scene.getWidth(), bounds.height() / (double) scene.getHeight())));
        int width = scene.getWidth() * scale;
        int height = scene.getHeight() * scale;
        int left = bounds.left + (bounds.width() - width) / 2;
        int top = bounds.top + (bounds.height() - height) / 2;
        target.set(left, top, left + width, top + height);
        canvas.save();
        canvas.clipRect(bounds);
        canvas.drawBitmap(scene, null, target, paint);
        canvas.restore();
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { paint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
