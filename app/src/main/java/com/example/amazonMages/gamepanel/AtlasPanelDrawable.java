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

/** 9-slice wooden/leaf forest frame backed by ui_panel (32x32) with integer-tiled edges. */
public final class AtlasPanelDrawable extends Drawable {
    private final Bitmap atlas;
    private final Paint paint = new Paint();
    private final Rect src = new Rect();
    private final Rect dst = new Rect();
    private final int[] sx = {0, 10, 22, 32};
    private final int[] sy = {0, 10, 22, 32};
    private final int[] dx = new int[4];
    private final int[] dy = new int[4];
    private final int pixelScale;

    public AtlasPanelDrawable(Context context) {
        atlas = ResourceManager.shared(context).bitmap(R.drawable.ui_panel, false);
        pixelScale = Math.max(2, Math.min(4, Math.round(context.getResources().getDisplayMetrics().density)));
        paint.setAntiAlias(false);
        paint.setFilterBitmap(false);
        paint.setDither(false);
    }

    @Override public void draw(Canvas canvas) {
        Rect b = getBounds();
        if (atlas == null || b.isEmpty()) return;
        int cap = Math.min(10 * pixelScale, Math.min(b.width() / 3, b.height() / 3));
        dx[0] = b.left; dx[1] = b.left + cap; dx[2] = b.right - cap; dx[3] = b.right;
        dy[0] = b.top; dy[1] = b.top + cap; dy[2] = b.bottom - cap; dy[3] = b.bottom;
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int dl = dx[c], dt = dy[r], dr = dx[c + 1], db = dy[r + 1];
                if (dr <= dl || db <= dt) continue;
                int sl = sx[c], st = sy[r], sr = sx[c + 1], sb = sy[r + 1];
                if (c == 1 && r != 1) {
                    int step = Math.max(4, (sr - sl) * pixelScale);
                    for (int x = dl; x < dr; x += step) {
                        int segW = Math.min(step, dr - x);
                        int srcStop = sl + Math.max(1, Math.round((sr - sl) * (segW / (float) step)));
                        src.set(sl, st, Math.min(sr, srcStop), sb);
                        dst.set(x, dt, x + segW, db);
                        canvas.drawBitmap(atlas, src, dst, paint);
                    }
                } else if (r == 1 && c != 1) {
                    int step = Math.max(4, (sb - st) * pixelScale);
                    for (int y = dt; y < db; y += step) {
                        int segH = Math.min(step, db - y);
                        int srcStop = st + Math.max(1, Math.round((sb - st) * (segH / (float) step)));
                        src.set(sl, st, sr, Math.min(sb, srcStop));
                        dst.set(dl, y, dr, y + segH);
                        canvas.drawBitmap(atlas, src, dst, paint);
                    }
                } else {
                    src.set(sl, st, sr, sb);
                    dst.set(dl, dt, dr, db);
                    canvas.drawBitmap(atlas, src, dst, paint);
                }
            }
        }
    }

    @Override public void setAlpha(int alpha) { paint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter colorFilter) { paint.setColorFilter(colorFilter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
